package io.github.ahmedabadawi.asn1java.core.codegen;

import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.ahmedabadawi.asn1java.core.ast.BooleanDefaultValueNode;
import io.github.ahmedabadawi.asn1java.core.ast.BooleanTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ChoiceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ConstraintNode;
import io.github.ahmedabadawi.asn1java.core.ast.DefaultValueNode;
import io.github.ahmedabadawi.asn1java.core.ast.EnumeratedDefaultValueNode;
import io.github.ahmedabadawi.asn1java.core.ast.EnumeratedTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.FieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerDefaultValueNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.BitStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.Ia5StringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.VisibleStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MaxBound;
import io.github.ahmedabadawi.asn1java.core.ast.NullTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MinBound;
import io.github.ahmedabadawi.asn1java.core.ast.NumberBound;
import io.github.ahmedabadawi.asn1java.core.ast.OctetStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceOfTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.StringDefaultValueNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeReferenceNode;
import io.github.ahmedabadawi.asn1java.core.ast.Utf8StringTypeNode;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

final class CodecGenerator {

  private static final String RUNTIME_PKG = "io.github.ahmedabadawi.asn1java.runtime.uper";

  private static final ClassName UPER_OUTPUT_STREAM =
      ClassName.get(RUNTIME_PKG, "UperOutputStream");
  private static final ClassName UPER_INPUT_STREAM = ClassName.get(RUNTIME_PKG, "UperInputStream");
  private static final ClassName UPER_CODEC_SUPPORT =
      ClassName.get(RUNTIME_PKG, "UperCodecSupport");
  private static final ClassName STRING = ClassName.get("java.lang", "String");
  private static final ClassName LIST = ClassName.get("java.util", "List");
  private static final ArrayTypeName BYTE_ARRAY = ArrayTypeName.of(TypeName.BYTE);

  private CodecGenerator() {
  }

  static JavaFile generate(String targetPackage, TypeAssignmentNode typeAssignment) {
    if (typeAssignment.type() instanceof ChoiceTypeNode choice) {
      return generateChoiceCodec(targetPackage, typeAssignment.name(), choice);
    }

    ClassName modelClass = ClassName.get(targetPackage, typeAssignment.name());
    List<EncodedField> fields = collectFields(typeAssignment);

    TypeSpec codec =
        TypeSpec.classBuilder(typeAssignment.name() + "Codec")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(buildEncodeMethod(modelClass, fields, targetPackage))
            .addMethod(buildEncodeIntoMethod(modelClass, fields, targetPackage))
            .addMethod(buildDecodeMethod(modelClass, fields, targetPackage))
            .addMethod(buildDecodeFromMethod(modelClass, fields, targetPackage))
            .build();
    return JavaFile.builder(targetPackage, codec).build();
  }

  private static JavaFile generateChoiceCodec(String targetPackage, String name,
      ChoiceTypeNode choice) {
    ClassName modelClass = ClassName.get(targetPackage, name);
    int bitCount = choiceIndexBitCount(choice);

    TypeSpec codec =
        TypeSpec.classBuilder(name + "Codec")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(buildEncodeMethod(modelClass, List.of(), targetPackage))
            .addMethod(buildChoiceEncodeIntoMethod(targetPackage, modelClass, choice, bitCount))
            .addMethod(buildDecodeMethod(modelClass, List.of(), targetPackage))
            .addMethod(buildChoiceDecodeFromMethod(targetPackage, modelClass, name, choice, bitCount))
            .build();
    return JavaFile.builder(targetPackage, codec).build();
  }

  private static MethodSpec buildChoiceEncodeIntoMethod(String targetPackage, ClassName modelClass,
      ChoiceTypeNode choice, int bitCount) {
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("encodeInto")
        .addParameter(ParameterSpec.builder(UPER_OUTPUT_STREAM, "out").build())
        .addParameter(ParameterSpec.builder(modelClass, "model").build());

    List<FieldNode> alternatives = choice.alternatives();
    for (int index = 0; index < alternatives.size(); index++) {
      FieldNode alternative = alternatives.get(index);
      ClassName variantClassName =
          modelClass.nestedClass(CodegenUtils.toJavaClassName(alternative.name()));
      if (alternative.type() instanceof NullTypeNode) {
        methodBuilder.beginControlFlow("if (model instanceof $T)", variantClassName)
            .addStatement("out.writeBits($L, $L)", index, bitCount)
            .addStatement("return")
            .endControlFlow();
      } else if (alternative.type() instanceof TypeReferenceNode ref) {
        methodBuilder.beginControlFlow("if (model instanceof $T variant)", variantClassName)
            .addStatement("out.writeBits($L, $L)", index, bitCount)
            .addStatement("new $T().encodeInto(out, variant.value())",
                ClassName.get(targetPackage, ref.typeName() + "Codec"))
            .addStatement("return")
            .endControlFlow();
      } else {
        throw new IllegalArgumentException(
            "CHOICE alternative type not supported: " + alternative.type());
      }
    }
    methodBuilder.addStatement("throw new $T($S + model)", IllegalArgumentException.class,
        "Unknown choice variant: ");
    return methodBuilder.build();
  }

  private static MethodSpec buildChoiceDecodeFromMethod(String targetPackage, ClassName modelClass,
      String name, ChoiceTypeNode choice, int bitCount) {
    CodeBlock.Builder switchBody = CodeBlock.builder();
    switchBody.add("return switch (index) {\n");
    List<FieldNode> alternatives = choice.alternatives();
    for (int index = 0; index < alternatives.size(); index++) {
      FieldNode alternative = alternatives.get(index);
      ClassName variantClassName =
          modelClass.nestedClass(CodegenUtils.toJavaClassName(alternative.name()));
      if (alternative.type() instanceof NullTypeNode) {
        switchBody.add("  case $L -> new $T();\n", index, variantClassName);
      } else if (alternative.type() instanceof TypeReferenceNode ref) {
        switchBody.add("  case $L -> new $T(new $T().decodeFrom(in));\n",
            index, variantClassName, ClassName.get(targetPackage, ref.typeName() + "Codec"));
      } else {
        throw new IllegalArgumentException(
            "CHOICE alternative type not supported: " + alternative.type());
      }
    }
    switchBody.add("  default -> throw new $T($S + index);\n", IllegalArgumentException.class,
        "Unknown " + name + " choice index: ");
    switchBody.add("};\n");

    return MethodSpec.methodBuilder("decodeFrom")
        .returns(modelClass)
        .addParameter(ParameterSpec.builder(UPER_INPUT_STREAM, "in").build())
        .addStatement("int index = (int) in.readBits($L)", bitCount)
        .addCode(switchBody.build())
        .build();
  }

  private static int choiceIndexBitCount(ChoiceTypeNode choice) {
    int count = choice.alternatives().size();
    return count <= 1 ? 0 : Integer.SIZE - Integer.numberOfLeadingZeros(count - 1);
  }

  private static MethodSpec buildEncodeMethod(ClassName modelClass, List<EncodedField> fields,
      String targetPackage) {
    return MethodSpec.methodBuilder("encode")
        .addModifiers(Modifier.PUBLIC)
        .returns(ArrayTypeName.of(TypeName.BYTE))
        .addParameter(ParameterSpec.builder(modelClass, "model").build())
        .addStatement("$T out = new $T()", UPER_OUTPUT_STREAM, UPER_OUTPUT_STREAM)
        .addStatement("encodeInto(out, model)")
        .addStatement("return out.toByteArray()")
        .build();
  }

  private static MethodSpec buildEncodeIntoMethod(ClassName modelClass, List<EncodedField> fields,
      String targetPackage) {
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("encodeInto")
        .addParameter(ParameterSpec.builder(UPER_OUTPUT_STREAM, "out").build())
        .addParameter(ParameterSpec.builder(modelClass, "model").build());

    fields.stream().filter(field -> field.optional() || field.hasDefault()).forEach(field ->
        methodBuilder.addStatement("out.writeBits($L ? 1 : 0, 1)", presenceCondition(field)));
    fields.forEach(field -> addEncodeStatement(methodBuilder, field, targetPackage));
    return methodBuilder.build();
  }

  private static CodeBlock presenceCondition(EncodedField field) {
    if (field.optional()) {
      return CodeBlock.of("model.$N() != null", field.name());
    }
    if (isStringEncoding(field.encoding())) {
      return CodeBlock.of("!model.$N().equals($S)", field.name(), field.defaultStringValue());
    }
    if (field.encoding() == Encoding.UNCONSTRAINED) {
      return CodeBlock.of("model.$N() != $LL", field.name(), field.defaultValue());
    }
    if (field.encoding() == Encoding.BOOLEAN) {
      return CodeBlock.of("model.$N() != $L", field.name(), field.defaultValue() != 0);
    }
    return CodeBlock.of("model.$N() != $L", field.name(), field.defaultValue());
  }

  private static boolean isStringEncoding(Encoding encoding) {
    return encoding == Encoding.UTF8_STRING || encoding == Encoding.IA5_STRING
        || encoding == Encoding.VISIBLE_STRING;
  }

  static void addFieldValidation(MethodSpec.Builder methodBuilder, EncodedField field) {
    if (field.encoding() == Encoding.UTF8_STRING) {
      CodeBlock.Builder checks = CodeBlock.builder();
      if (field.lowerBound() > 0) {
        checks.beginControlFlow(
                "if ($N.getBytes($T.UTF_8).length < $L)",
                field.name(), ClassName.get("java.nio.charset", "StandardCharsets"),
                field.lowerBound())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " length must be >= " + field.lowerBound())
            .endControlFlow();
      }
      if (field.upperBound() != Long.MAX_VALUE) {
        checks.beginControlFlow(
                "if ($N.getBytes($T.UTF_8).length > $L)",
                field.name(), ClassName.get("java.nio.charset", "StandardCharsets"),
                (int) field.upperBound())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " length must be <= " + (int) field.upperBound())
            .endControlFlow();
      }
      addNullableFieldChecks(methodBuilder, field, checks.build());
    } else if (field.encoding() == Encoding.IA5_STRING
        || field.encoding() == Encoding.VISIBLE_STRING) {
      CodeBlock.Builder checks = CodeBlock.builder();
      if (field.lowerBound() > 0) {
        checks.beginControlFlow("if ($N.length() < $L)",
                field.name(), field.lowerBound())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " length must be >= " + field.lowerBound())
            .endControlFlow();
      }
      if (field.upperBound() != Long.MAX_VALUE) {
        checks.beginControlFlow("if ($N.length() > $L)",
                field.name(), (int) field.upperBound())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " length must be <= " + (int) field.upperBound())
            .endControlFlow();
      }
      addNullableFieldChecks(methodBuilder, field, checks.build());
    } else if (field.encoding() == Encoding.BIT_STRING) {
      CodeBlock.Builder checks = CodeBlock.builder();
      if (field.bitCount() == 0 && field.lowerBound() > 0) {
        checks.beginControlFlow(
                "if ($N.length * 8 != $L)", field.name(), field.lowerBound())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " must be exactly " + field.lowerBound() + " bits")
            .endControlFlow();
      }
      addNullableFieldChecks(methodBuilder, field, checks.build());
    } else if (field.encoding() == Encoding.OCTET_STRING) {
      CodeBlock.Builder checks = CodeBlock.builder();
      if (field.lowerBound() > 0) {
        checks.beginControlFlow("if ($N.length < $L)",
                field.name(), field.lowerBound())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " length must be >= " + field.lowerBound())
            .endControlFlow();
      }
      if (field.upperBound() != Long.MAX_VALUE) {
        checks.beginControlFlow("if ($N.length > $L)",
                field.name(), (int) field.upperBound())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " length must be <= " + (int) field.upperBound())
            .endControlFlow();
      }
      addNullableFieldChecks(methodBuilder, field, checks.build());
    } else if (field.encoding() == Encoding.UNCONSTRAINED) {
      CodeBlock.Builder checks = CodeBlock.builder();
      if (field.upperBound() != Long.MAX_VALUE) {
        checks.beginControlFlow("if ($N > $LL)", field.name(), field.upperBound())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " must be <= " + field.upperBound())
            .endControlFlow();
      }
      addOptionalGuardedChecks(methodBuilder, field, checks.build());
    } else if (field.encoding() == Encoding.TYPE_REFERENCE) {
      addNullableFieldChecks(methodBuilder, field, CodeBlock.of(""));
    } else if (field.encoding() == Encoding.SEQUENCE_OF) {
      CodeBlock.Builder checks = CodeBlock.builder();
      if (field.upperBound() != Long.MAX_VALUE) {
        if (field.bitCount() == 0) {
          checks.beginControlFlow("if ($N.size() != $L)", field.name(), field.lowerBound())
              .addStatement("throw new $T($S)", IllegalArgumentException.class,
                  field.name() + " must have exactly " + field.lowerBound() + " elements")
              .endControlFlow();
        } else {
          checks.beginControlFlow("if ($N.size() < $L || $N.size() > $L)", field.name(),
                  field.lowerBound(), field.name(), (int) field.upperBound())
              .addStatement("throw new $T($S)", IllegalArgumentException.class,
                  field.name() + " must have between " + field.lowerBound() + " and "
                      + (int) field.upperBound() + " elements")
              .endControlFlow();
        }
      }
      addNullableFieldChecks(methodBuilder, field, checks.build());
      addElementValidation(methodBuilder, field);
    } else if (field.encoding() != Encoding.BOOLEAN) {
      CodeBlock.Builder checks = CodeBlock.builder();
      checks.beginControlFlow("if ($N < $L)", field.name(), field.lowerBound())
          .addStatement("throw new $T($S)", IllegalArgumentException.class,
              field.name() + " must be >= " + field.lowerBound())
          .endControlFlow();
      if (field.upperBound() != Long.MAX_VALUE) {
        checks.beginControlFlow("if ($N > $L)", field.name(), (int) field.upperBound())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " must be <= " + (int) field.upperBound())
            .endControlFlow();
      }
      addOptionalGuardedChecks(methodBuilder, field, checks.build());
    }
  }

  private static void addNullableFieldChecks(MethodSpec.Builder methodBuilder, EncodedField field,
      CodeBlock checks) {
    if (field.optional()) {
      addOptionalGuardedChecks(methodBuilder, field, checks);
    } else {
      methodBuilder.beginControlFlow("if ($N == null)", field.name())
          .addStatement("throw new $T($S)", IllegalArgumentException.class,
              field.name() + " must not be null")
          .endControlFlow();
      methodBuilder.addCode(checks);
    }
  }

  private static void addOptionalGuardedChecks(MethodSpec.Builder methodBuilder, EncodedField field,
      CodeBlock checks) {
    if (field.optional()) {
      if (!checks.isEmpty()) {
        methodBuilder.beginControlFlow("if ($N != null)", field.name())
            .addCode(checks)
            .endControlFlow();
      }
    } else {
      methodBuilder.addCode(checks);
    }
  }

  // Reference-typed elements (TYPE_REFERENCE) validate themselves via their own record's
  // compact constructor when each element was built, so no extra loop is needed for them.
  // Nested SEQUENCE_OF/BOOLEAN/ZERO_RANGE elements have no meaningful per-element bound to
  // check here either (BOOLEAN has no constraint; ZERO_RANGE is fully determined).
  private static void addElementValidation(MethodSpec.Builder methodBuilder, EncodedField field) {
    EncodedField elementField = field.elementField();
    if (elementField.encoding() == Encoding.TYPE_REFERENCE
        || elementField.encoding() == Encoding.SEQUENCE_OF
        || elementField.encoding() == Encoding.BOOLEAN
        || elementField.encoding() == Encoding.ZERO_RANGE) {
      return;
    }
    if (field.optional()) {
      methodBuilder.beginControlFlow("if ($N != null)", field.name());
    }
    TypeName elementType = primitiveElementJavaType(elementField.encoding());
    methodBuilder.beginControlFlow("for ($T $N : $N)", elementType, elementField.name(),
        field.name());
    addFieldValidation(methodBuilder, elementField);
    methodBuilder.endControlFlow();
    if (field.optional()) {
      methodBuilder.endControlFlow();
    }
  }

  private static TypeName primitiveElementJavaType(Encoding encoding) {
    return switch (encoding) {
      case SEMI_CONSTRAINED, CONSTRAINED, ZERO_RANGE, ENUMERATED -> TypeName.INT;
      case UNCONSTRAINED -> TypeName.LONG;
      case BOOLEAN -> TypeName.BOOLEAN;
      case UTF8_STRING, IA5_STRING, VISIBLE_STRING -> STRING;
      case OCTET_STRING, BIT_STRING -> BYTE_ARRAY;
      case TYPE_REFERENCE, SEQUENCE_OF -> throw new IllegalStateException(
          "not a primitive element encoding: " + encoding);
    };
  }

  private static MethodSpec buildDecodeMethod(ClassName modelClass, List<EncodedField> fields,
      String targetPackage) {
    return MethodSpec.methodBuilder("decode")
        .addModifiers(Modifier.PUBLIC)
        .returns(modelClass)
        .addParameter(ParameterSpec.builder(ArrayTypeName.of(TypeName.BYTE), "data").build())
        .addStatement("return decodeFrom(new $T(data))", UPER_INPUT_STREAM)
        .build();
  }

  private static MethodSpec buildDecodeFromMethod(ClassName modelClass, List<EncodedField> fields,
      String targetPackage) {
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("decodeFrom")
        .returns(modelClass)
        .addParameter(ParameterSpec.builder(UPER_INPUT_STREAM, "in").build());

    fields.stream().filter(field -> field.optional() || field.hasDefault()).forEach(field ->
        methodBuilder.addStatement("boolean $LPresent = in.readBits(1) != 0", field.name()));
    fields.forEach(field -> addDecodeStatement(methodBuilder, field, targetPackage));

    String args = fields.stream()
        .map(EncodedField::name)
        .collect(Collectors.joining(", "));
    methodBuilder.addStatement("return new $T($L)", modelClass, args);
    return methodBuilder.build();
  }

  private static void addEncodeStatement(MethodSpec.Builder builder, EncodedField field,
      String targetPackage) {
    if (field.optional() || field.hasDefault()) {
      builder.beginControlFlow("if ($L)", presenceCondition(field));
    }
    if (field.encoding() == Encoding.SEQUENCE_OF) {
      EncodedField elementField = field.elementField();
      TypeName elementType = elementJavaType(elementField, targetPackage);
      CodeBlock elementExpr =
          encodeExpression(elementField, targetPackage, CodeBlock.of("item"), "stream");
      builder.addStatement(
          "$T.encodeSequenceOf(out, model.$N(), $L, $LL, ($T stream, $T item) -> $L)",
          UPER_CODEC_SUPPORT, field.name(), field.lowerBound(), field.upperBound(),
          UPER_OUTPUT_STREAM, elementType, elementExpr);
    } else if (field.encoding() != Encoding.ZERO_RANGE) {
      builder.addStatement(
          encodeExpression(field, targetPackage, CodeBlock.of("model.$N()", field.name()), "out"));
    }
    if (field.optional() || field.hasDefault()) {
      builder.endControlFlow();
    }
  }

  private static CodeBlock encodeExpression(EncodedField field, String targetPackage,
      CodeBlock valueExpr, String streamVar) {
    return switch (field.encoding()) {
      case SEMI_CONSTRAINED -> field.lowerBound() == 0
          ? CodeBlock.of("$T.encodeSemiConstrainedInt($L, $L)", UPER_CODEC_SUPPORT, streamVar,
              valueExpr)
          : CodeBlock.of("$T.encodeSemiConstrainedInt($L, $L - $L)", UPER_CODEC_SUPPORT, streamVar,
              valueExpr, field.lowerBound());
      case CONSTRAINED -> field.lowerBound() == 0
          ? CodeBlock.of("$L.writeBits($L, $L)", streamVar, valueExpr, field.bitCount())
          : CodeBlock.of("$L.writeBits($L - $L, $L)", streamVar, valueExpr, field.lowerBound(),
              field.bitCount());
      case ZERO_RANGE -> CodeBlock.of("");
      case UNCONSTRAINED -> CodeBlock.of("$T.encodeUnconstrainedInt($L, $L)", UPER_CODEC_SUPPORT,
          streamVar, valueExpr);
      case OCTET_STRING -> field.bitCount() == 0
          ? CodeBlock.of("$T.encodeFixedOctetString($L, $L)", UPER_CODEC_SUPPORT, streamVar,
              valueExpr)
          : CodeBlock.of("$T.encodeOctetString($L, $L, $L, $L)", UPER_CODEC_SUPPORT, streamVar,
              valueExpr, field.lowerBound(), (int) field.upperBound());
      case BIT_STRING -> CodeBlock.of("$T.encodeBitString($L, $L, $L)", UPER_CODEC_SUPPORT,
          streamVar, valueExpr, field.lowerBound());
      case IA5_STRING -> CodeBlock.of("$T.encodeIa5String($L, $L, $L, $L)", UPER_CODEC_SUPPORT,
          streamVar, valueExpr, field.lowerBound(), (int) field.upperBound());
      case VISIBLE_STRING -> CodeBlock.of("$T.encodeVisibleString($L, $L, $L, $L)",
          UPER_CODEC_SUPPORT, streamVar, valueExpr, field.lowerBound(), (int) field.upperBound());
      case BOOLEAN -> CodeBlock.of("$L.writeBits($L ? 1 : 0, 1)", streamVar, valueExpr);
      case UTF8_STRING -> CodeBlock.of("$T.encodeUtf8String($L, $L)", UPER_CODEC_SUPPORT,
          streamVar, valueExpr);
      case ENUMERATED -> CodeBlock.of("$L.writeBits($L, $L)", streamVar, valueExpr,
          field.bitCount());
      case TYPE_REFERENCE -> CodeBlock.of("new $T().encodeInto($L, $L)",
          ClassName.get(targetPackage, field.referencedTypeName() + "Codec"), streamVar,
          valueExpr);
      case SEQUENCE_OF -> throw new IllegalArgumentException(
          "nested SEQUENCE OF element not supported");
    };
  }

  private static void addDecodeStatement(MethodSpec.Builder builder, EncodedField field,
      String targetPackage) {
    switch (field.encoding()) {
      case SEMI_CONSTRAINED -> {
        if (field.lowerBound() == 0) {
          addDecodeAssignment(builder, field, TypeName.INT,
              "(int) $T.decodeSemiConstrainedInt(in)", UPER_CODEC_SUPPORT);
        } else {
          addDecodeAssignment(builder, field, TypeName.INT,
              "(int) $T.decodeSemiConstrainedInt(in) + $L", UPER_CODEC_SUPPORT,
              field.lowerBound());
        }
      }
      case CONSTRAINED -> {
        if (field.lowerBound() == 0) {
          addDecodeAssignment(builder, field, TypeName.INT, "(int) in.readBits($L)",
              field.bitCount());
        } else {
          addDecodeAssignment(builder, field, TypeName.INT, "(int) in.readBits($L) + $L",
              field.bitCount(), field.lowerBound());
        }
      }
      case ZERO_RANGE ->
          addDecodeAssignment(builder, field, TypeName.INT, "$L", field.lowerBound());
      case UNCONSTRAINED -> addDecodeAssignment(builder, field, TypeName.LONG,
          "$T.decodeUnconstrainedInt(in)", UPER_CODEC_SUPPORT);
      case OCTET_STRING -> {
        if (field.bitCount() == 0) {
          addDecodeAssignment(builder, field, BYTE_ARRAY, "$T.decodeFixedOctetString(in, $L)",
              UPER_CODEC_SUPPORT, field.lowerBound());
        } else {
          addDecodeAssignment(builder, field, BYTE_ARRAY, "$T.decodeOctetString(in, $L, $L)",
              UPER_CODEC_SUPPORT, field.lowerBound(), (int) field.upperBound());
        }
      }
      case BIT_STRING -> addDecodeAssignment(builder, field, BYTE_ARRAY,
          "$T.decodeBitString(in, $L)", UPER_CODEC_SUPPORT, field.lowerBound());
      case IA5_STRING -> addDecodeAssignment(builder, field, STRING,
          "$T.decodeIa5String(in, $L, $L)", UPER_CODEC_SUPPORT, field.lowerBound(),
          (int) field.upperBound());
      case VISIBLE_STRING -> addDecodeAssignment(builder, field, STRING,
          "$T.decodeVisibleString(in, $L, $L)", UPER_CODEC_SUPPORT, field.lowerBound(),
          (int) field.upperBound());
      case BOOLEAN -> addDecodeAssignment(builder, field, TypeName.BOOLEAN,
          "in.readBits(1) != 0");
      case UTF8_STRING -> addDecodeAssignment(builder, field, STRING, "$T.decodeUtf8String(in)",
          UPER_CODEC_SUPPORT);
      case ENUMERATED -> addDecodeAssignment(builder, field, TypeName.INT,
          "(int) in.readBits($L)", field.bitCount());
      case TYPE_REFERENCE -> addDecodeAssignment(builder, field,
          ClassName.get(targetPackage, field.referencedTypeName()), "new $T().decodeFrom(in)",
          ClassName.get(targetPackage, field.referencedTypeName() + "Codec"));
      case SEQUENCE_OF -> {
        EncodedField elementField = field.elementField();
        TypeName elementType = elementJavaType(elementField, targetPackage);
        TypeName listType = ParameterizedTypeName.get(LIST, elementType.box());
        CodeBlock elementExpr = decodeExpression(elementField, targetPackage, "elementIn");
        addDecodeAssignment(builder, field, listType,
            "$T.decodeSequenceOf(in, $L, $LL, ($T elementIn) -> $L)", UPER_CODEC_SUPPORT,
            field.lowerBound(), field.upperBound(), UPER_INPUT_STREAM, elementExpr);
      }
    }
  }

  private static CodeBlock decodeExpression(EncodedField field, String targetPackage,
      String streamVar) {
    return switch (field.encoding()) {
      case SEMI_CONSTRAINED -> field.lowerBound() == 0
          ? CodeBlock.of("(int) $T.decodeSemiConstrainedInt($L)", UPER_CODEC_SUPPORT, streamVar)
          : CodeBlock.of("(int) $T.decodeSemiConstrainedInt($L) + $L", UPER_CODEC_SUPPORT,
              streamVar, field.lowerBound());
      case CONSTRAINED -> field.lowerBound() == 0
          ? CodeBlock.of("(int) $L.readBits($L)", streamVar, field.bitCount())
          : CodeBlock.of("(int) $L.readBits($L) + $L", streamVar, field.bitCount(),
              field.lowerBound());
      case ZERO_RANGE -> CodeBlock.of("$L", field.lowerBound());
      case UNCONSTRAINED -> CodeBlock.of("$T.decodeUnconstrainedInt($L)", UPER_CODEC_SUPPORT,
          streamVar);
      case OCTET_STRING -> field.bitCount() == 0
          ? CodeBlock.of("$T.decodeFixedOctetString($L, $L)", UPER_CODEC_SUPPORT, streamVar,
              field.lowerBound())
          : CodeBlock.of("$T.decodeOctetString($L, $L, $L)", UPER_CODEC_SUPPORT, streamVar,
              field.lowerBound(), (int) field.upperBound());
      case BIT_STRING -> CodeBlock.of("$T.decodeBitString($L, $L)", UPER_CODEC_SUPPORT, streamVar,
          field.lowerBound());
      case IA5_STRING -> CodeBlock.of("$T.decodeIa5String($L, $L, $L)", UPER_CODEC_SUPPORT,
          streamVar, field.lowerBound(), (int) field.upperBound());
      case VISIBLE_STRING -> CodeBlock.of("$T.decodeVisibleString($L, $L, $L)", UPER_CODEC_SUPPORT,
          streamVar, field.lowerBound(), (int) field.upperBound());
      case BOOLEAN -> CodeBlock.of("$L.readBits(1) != 0", streamVar);
      case UTF8_STRING -> CodeBlock.of("$T.decodeUtf8String($L)", UPER_CODEC_SUPPORT, streamVar);
      case ENUMERATED -> CodeBlock.of("(int) $L.readBits($L)", streamVar, field.bitCount());
      case TYPE_REFERENCE -> CodeBlock.of("new $T().decodeFrom($L)",
          ClassName.get(targetPackage, field.referencedTypeName() + "Codec"), streamVar);
      case SEQUENCE_OF -> throw new IllegalArgumentException(
          "nested SEQUENCE OF element not supported");
    };
  }

  private static TypeName elementJavaType(EncodedField elementField, String targetPackage) {
    return switch (elementField.encoding()) {
      case SEMI_CONSTRAINED, CONSTRAINED, ZERO_RANGE, ENUMERATED -> TypeName.INT;
      case UNCONSTRAINED -> TypeName.LONG;
      case BOOLEAN -> TypeName.BOOLEAN;
      case UTF8_STRING, IA5_STRING, VISIBLE_STRING -> STRING;
      case OCTET_STRING, BIT_STRING -> BYTE_ARRAY;
      case TYPE_REFERENCE -> ClassName.get(targetPackage, elementField.referencedTypeName());
      case SEQUENCE_OF -> ParameterizedTypeName.get(LIST,
          elementJavaType(elementField.elementField(), targetPackage).box());
    };
  }

  private static void addDecodeAssignment(MethodSpec.Builder builder, EncodedField field,
      TypeName mandatoryType, String rhsFormat, Object... rhsArgs) {
    Object[] args = new Object[rhsArgs.length + 2];
    args[1] = field.name();
    System.arraycopy(rhsArgs, 0, args, 2, rhsArgs.length);
    if (field.optional()) {
      args[0] = mandatoryType.box();
      builder.addStatement(
          "$T $N = " + field.name() + "Present ? (" + rhsFormat + ") : null", args);
    } else if (field.hasDefault() && isStringEncoding(field.encoding())) {
      args[0] = mandatoryType;
      Object[] withDefault = Arrays.copyOf(args, args.length + 1);
      withDefault[args.length] = field.defaultStringValue();
      builder.addStatement(
          "$T $N = " + field.name() + "Present ? (" + rhsFormat + ") : $S", withDefault);
    } else if (field.hasDefault()) {
      args[0] = mandatoryType;
      builder.addStatement(
          "$T $N = " + field.name() + "Present ? (" + rhsFormat + ") : "
              + defaultLiteral(field, mandatoryType), args);
    } else {
      args[0] = mandatoryType;
      builder.addStatement("$T $N = " + rhsFormat, args);
    }
  }

  private static String defaultLiteral(EncodedField field, TypeName mandatoryType) {
    if (mandatoryType.equals(TypeName.BOOLEAN)) {
      return field.defaultValue() != 0 ? "true" : "false";
    }
    if (mandatoryType.equals(TypeName.LONG)) {
      return field.defaultValue() + "L";
    }
    return String.valueOf(field.defaultValue());
  }

  static List<EncodedField> collectFields(TypeAssignmentNode typeAssignment) {
    return switch (typeAssignment.type()) {
      case SequenceTypeNode seq -> seq.fields()
          .stream()
          .filter(field -> !(field.type() instanceof NullTypeNode))
          .map(field -> {
            String javaName = CodegenUtils.toJavaFieldName(field.name());
            EncodedField encoded = toEncodedField(javaName, field.type());
            if (field.optional()) {
              return encoded.withOptional(true);
            }
            return field.defaultValue() != null
                ? applyDefault(encoded, field.type(), field.defaultValue())
                : encoded;
          })
          .collect(Collectors.toList());
      case ChoiceTypeNode ignored -> throw new IllegalStateException(
          "choice types are handled via generateChoiceCodec");
      case IntegerTypeNode intType -> List.of(toEncodedField("value", intType));
      case BooleanTypeNode ignored -> List.of(new EncodedField("value", 0, Encoding.BOOLEAN, 1));
      case Utf8StringTypeNode utf8Type -> List.of(toEncodedField("value", utf8Type));
      case OctetStringTypeNode octetType -> List.of(toEncodedField("value", octetType));
      case BitStringTypeNode bitType -> List.of(toEncodedField("value", bitType));
      case NullTypeNode ignored -> List.of();
      case Ia5StringTypeNode ia5Type -> List.of(toEncodedField("value", ia5Type));
      case VisibleStringTypeNode visibleType -> List.of(toEncodedField("value", visibleType));
      case EnumeratedTypeNode enumType -> List.of(toEncodedField("value", enumType));
      case SequenceOfTypeNode sequenceOfType -> List.of(toEncodedField("value", sequenceOfType));
      case TypeReferenceNode ignored ->
          throw new IllegalArgumentException(
              "top-level TypeReferenceNode is not a valid type assignment body");
    };
  }

  private static EncodedField toEncodedField(String javaName, TypeNode type) {
    return switch (type) {
      case IntegerTypeNode intType -> toEncodedField(javaName, intType);
      case BooleanTypeNode ignored -> new EncodedField(javaName, 0, Encoding.BOOLEAN, 1);
      case Utf8StringTypeNode utf8Type -> toEncodedField(javaName, utf8Type);
      case OctetStringTypeNode octetType -> toEncodedField(javaName, octetType);
      case BitStringTypeNode bitType -> toEncodedField(javaName, bitType);
      case NullTypeNode ignored ->
          throw new IllegalArgumentException("SEQUENCE OF NULL element is not supported");
      case Ia5StringTypeNode ia5Type -> toEncodedField(javaName, ia5Type);
      case VisibleStringTypeNode visibleType -> toEncodedField(javaName, visibleType);
      case SequenceTypeNode ignored ->
          throw new IllegalArgumentException("nested SEQUENCE not supported");
      case ChoiceTypeNode ignored ->
          throw new IllegalArgumentException("nested CHOICE not supported");
      case EnumeratedTypeNode enumType -> toEncodedField(javaName, enumType);
      case SequenceOfTypeNode sequenceOfType -> toEncodedField(javaName, sequenceOfType);
      case TypeReferenceNode ref ->
          new EncodedField(javaName, 0, Encoding.TYPE_REFERENCE, 0, Long.MAX_VALUE,
              ref.typeName());
    };
  }

  private static EncodedField toEncodedField(String name, SequenceOfTypeNode sequenceOfType) {
    EncodedField elementField = toEncodedField(name + "Element", sequenceOfType.elementType());
    if (sequenceOfType.sizeConstraint().isEmpty()) {
      return new EncodedField(name, 0, Encoding.SEQUENCE_OF, 0, Long.MAX_VALUE, elementField);
    }
    ConstraintNode size = sequenceOfType.sizeConstraint().get();
    int lb = ((NumberBound) size.lowerBound()).value();
    int ub = ((NumberBound) size.upperBound()).value();
    int range = ub - lb;
    int bitCount = range == 0 ? 0 : Integer.SIZE - Integer.numberOfLeadingZeros(range);
    return new EncodedField(name, lb, Encoding.SEQUENCE_OF, bitCount, ub, elementField);
  }

  private static EncodedField toEncodedField(String name, Ia5StringTypeNode ia5Type) {
    if (ia5Type.sizeConstraint().isEmpty()) {
      return new EncodedField(name, 0, Encoding.IA5_STRING, 0);
    }
    ConstraintNode size = ia5Type.sizeConstraint().get();
    int lb = ((NumberBound) size.lowerBound()).value();
    int ub = ((NumberBound) size.upperBound()).value();
    return new EncodedField(name, lb, Encoding.IA5_STRING, 0, ub);
  }

  private static EncodedField toEncodedField(String name, VisibleStringTypeNode visibleType) {
    if (visibleType.sizeConstraint().isEmpty()) {
      return new EncodedField(name, 0, Encoding.VISIBLE_STRING, 0);
    }
    ConstraintNode size = visibleType.sizeConstraint().get();
    int lb = ((NumberBound) size.lowerBound()).value();
    int ub = ((NumberBound) size.upperBound()).value();
    return new EncodedField(name, lb, Encoding.VISIBLE_STRING, 0, ub);
  }

  private static EncodedField toEncodedField(String name, Utf8StringTypeNode utf8Type) {
    if (utf8Type.sizeConstraint().isEmpty()) {
      return new EncodedField(name, 0, Encoding.UTF8_STRING, 0);
    }
    ConstraintNode size = utf8Type.sizeConstraint().get();
    int lb = ((NumberBound) size.lowerBound()).value();
    int ub = ((NumberBound) size.upperBound()).value();
    return new EncodedField(name, lb, Encoding.UTF8_STRING, 0, ub);
  }

  private static EncodedField toEncodedField(String name, BitStringTypeNode bitType) {
    if (bitType.sizeConstraint().isEmpty()) {
      return new EncodedField(name, 0, Encoding.BIT_STRING, 0, Long.MAX_VALUE);
    }
    ConstraintNode size = bitType.sizeConstraint().get();
    int lb = ((NumberBound) size.lowerBound()).value();
    int ub = ((NumberBound) size.upperBound()).value();
    int range = ub - lb;
    int rangeBitCount = range == 0 ? 0 : Integer.SIZE - Integer.numberOfLeadingZeros(range);
    return new EncodedField(name, lb, Encoding.BIT_STRING, rangeBitCount, ub);
  }

  private static EncodedField toEncodedField(String name, OctetStringTypeNode octetType) {
    if (octetType.sizeConstraint().isEmpty()) {
      return new EncodedField(name, 0, Encoding.OCTET_STRING, 0, Long.MAX_VALUE);
    }
    ConstraintNode size = octetType.sizeConstraint().get();
    int lb = ((NumberBound) size.lowerBound()).value();
    int ub = ((NumberBound) size.upperBound()).value();
    int range = ub - lb;
    int bitCount = range == 0 ? 0 : Integer.SIZE - Integer.numberOfLeadingZeros(range);
    return new EncodedField(name, lb, Encoding.OCTET_STRING, bitCount, ub);
  }

  private static EncodedField toEncodedField(String name, EnumeratedTypeNode enumType) {
    int count = enumType.values().size();
    int bitCount = count <= 1 ? 0 : Integer.SIZE - Integer.numberOfLeadingZeros(count - 1);
    return new EncodedField(name, 0, Encoding.ENUMERATED, bitCount, count - 1);
  }

  private static EncodedField applyDefault(EncodedField encoded, TypeNode type,
      DefaultValueNode defaultValue) {
    return switch (defaultValue) {
      case IntegerDefaultValueNode intDefault -> encoded.withDefault(intDefault.value());
      case BooleanDefaultValueNode boolDefault -> encoded.withDefault(boolDefault.value() ? 1 : 0);
      case EnumeratedDefaultValueNode enumDefault -> encoded.withDefault(
          ((EnumeratedTypeNode) type).values().indexOf(enumDefault.value()));
      case StringDefaultValueNode stringDefault -> encoded.withStringDefault(stringDefault.value());
    };
  }

  private static EncodedField toEncodedField(String name, IntegerTypeNode intType) {
    ConstraintNode constraint = intType.constraint();
    if (constraint == null) {
      return new EncodedField(name, 0, Encoding.SEMI_CONSTRAINED, 0);
    }
    return switch (constraint.lowerBound()) {
      case MinBound ignored -> {
        long upperBound = switch (constraint.upperBound()) {
          case NumberBound nb -> (long) nb.value();
          case MaxBound ignored2 -> Long.MAX_VALUE;
          case MinBound ignored2 -> throw new IllegalArgumentException(
              "upper bound cannot be MIN for field " + name);
        };
        yield new EncodedField(name, 0, Encoding.UNCONSTRAINED, 0, upperBound);
      }
      case NumberBound lowerBound -> {
        int lb = lowerBound.value();
        yield switch (constraint.upperBound()) {
          case MaxBound ignored -> new EncodedField(name, lb, Encoding.SEMI_CONSTRAINED, 0);
          case NumberBound numberBound -> {
            int range = numberBound.value() - lb;
            if (range == 0) {
              yield new EncodedField(name, lb, Encoding.ZERO_RANGE, 0, lb);
            }
            int bitCount = Integer.SIZE - Integer.numberOfLeadingZeros(range);
            yield new EncodedField(name, lb, Encoding.CONSTRAINED, bitCount, numberBound.value());
          }
          case MinBound ignored -> throw new IllegalArgumentException(
              "upper bound cannot be MIN for field " + name);
        };
      }
      case MaxBound ignored -> throw new IllegalArgumentException(
          "lower bound cannot be MAX for field " + name);
    };
  }

  enum Encoding {
    SEMI_CONSTRAINED, CONSTRAINED, ZERO_RANGE, BOOLEAN, UTF8_STRING, ENUMERATED, UNCONSTRAINED,
    OCTET_STRING, BIT_STRING, IA5_STRING, VISIBLE_STRING, TYPE_REFERENCE, SEQUENCE_OF
  }

  record EncodedField(String name, int lowerBound, Encoding encoding, int bitCount,
      long upperBound, String referencedTypeName, boolean optional, boolean hasDefault,
      long defaultValue, String defaultStringValue, EncodedField elementField) {
    EncodedField(String name, int lowerBound, Encoding encoding, int bitCount) {
      this(name, lowerBound, encoding, bitCount, Long.MAX_VALUE, null, false, false, 0, null,
          null);
    }

    EncodedField(String name, int lowerBound, Encoding encoding, int bitCount, long upperBound) {
      this(name, lowerBound, encoding, bitCount, upperBound, null, false, false, 0, null, null);
    }

    EncodedField(String name, int lowerBound, Encoding encoding, int bitCount, long upperBound,
        String referencedTypeName) {
      this(name, lowerBound, encoding, bitCount, upperBound, referencedTypeName, false, false, 0,
          null, null);
    }

    EncodedField(String name, int lowerBound, Encoding encoding, int bitCount, long upperBound,
        EncodedField elementField) {
      this(name, lowerBound, encoding, bitCount, upperBound, null, false, false, 0, null,
          elementField);
    }

    EncodedField withOptional(boolean optionalValue) {
      return new EncodedField(name, lowerBound, encoding, bitCount, upperBound, referencedTypeName,
          optionalValue, hasDefault, defaultValue, defaultStringValue, elementField);
    }

    EncodedField withDefault(long defaultValueArg) {
      return new EncodedField(name, lowerBound, encoding, bitCount, upperBound, referencedTypeName,
          optional, true, defaultValueArg, null, elementField);
    }

    EncodedField withStringDefault(String defaultStringValueArg) {
      return new EncodedField(name, lowerBound, encoding, bitCount, upperBound, referencedTypeName,
          optional, true, 0, defaultStringValueArg, elementField);
    }
  }
}
