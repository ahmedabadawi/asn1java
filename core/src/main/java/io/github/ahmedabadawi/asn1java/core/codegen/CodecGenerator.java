package io.github.ahmedabadawi.asn1java.core.codegen;

import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.ahmedabadawi.asn1java.core.ast.BooleanTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ConstraintNode;
import io.github.ahmedabadawi.asn1java.core.ast.EnumeratedTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.BitStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.Ia5StringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.VisibleStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MaxBound;
import io.github.ahmedabadawi.asn1java.core.ast.NullTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MinBound;
import io.github.ahmedabadawi.asn1java.core.ast.NumberBound;
import io.github.ahmedabadawi.asn1java.core.ast.OctetStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeReferenceNode;
import io.github.ahmedabadawi.asn1java.core.ast.Utf8StringTypeNode;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.stream.Collectors;

final class CodecGenerator {

  private static final String RUNTIME_PKG = "io.github.ahmedabadawi.asn1java.runtime.uper";

  private static final ClassName UPER_OUTPUT_STREAM =
      ClassName.get(RUNTIME_PKG, "UperOutputStream");
  private static final ClassName UPER_INPUT_STREAM = ClassName.get(RUNTIME_PKG, "UperInputStream");
  private static final ClassName UPER_CODEC_SUPPORT =
      ClassName.get(RUNTIME_PKG, "UperCodecSupport");

  private CodecGenerator() {
  }

  static JavaFile generate(String targetPackage, TypeAssignmentNode typeAssignment) {
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

    for (EncodedField field : fields) {
      if (field.encoding() == Encoding.UTF8_STRING) {
        methodBuilder.beginControlFlow("if (model.$N() == null)", field.name())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " must not be null")
            .endControlFlow();
        if (field.lowerBound() > 0) {
          methodBuilder.beginControlFlow(
                  "if (model.$N().getBytes($T.UTF_8).length < $L)",
                  field.name(), ClassName.get("java.nio.charset", "StandardCharsets"),
                  field.lowerBound())
              .addStatement("throw new $T($S)", IllegalArgumentException.class,
                  field.name() + " length must be >= " + field.lowerBound())
              .endControlFlow();
        }
        if (field.upperBound() != Long.MAX_VALUE) {
          methodBuilder.beginControlFlow(
                  "if (model.$N().getBytes($T.UTF_8).length > $L)",
                  field.name(), ClassName.get("java.nio.charset", "StandardCharsets"),
                  (int) field.upperBound())
              .addStatement("throw new $T($S)", IllegalArgumentException.class,
                  field.name() + " length must be <= " + (int) field.upperBound())
              .endControlFlow();
        }
      } else if (field.encoding() == Encoding.IA5_STRING
          || field.encoding() == Encoding.VISIBLE_STRING) {
        methodBuilder.beginControlFlow("if (model.$N() == null)", field.name())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " must not be null")
            .endControlFlow();
        if (field.lowerBound() > 0) {
          methodBuilder.beginControlFlow("if (model.$N().length() < $L)",
                  field.name(), field.lowerBound())
              .addStatement("throw new $T($S)", IllegalArgumentException.class,
                  field.name() + " length must be >= " + field.lowerBound())
              .endControlFlow();
        }
        if (field.upperBound() != Long.MAX_VALUE) {
          methodBuilder.beginControlFlow("if (model.$N().length() > $L)",
                  field.name(), (int) field.upperBound())
              .addStatement("throw new $T($S)", IllegalArgumentException.class,
                  field.name() + " length must be <= " + (int) field.upperBound())
              .endControlFlow();
        }
      } else if (field.encoding() == Encoding.BIT_STRING) {
        methodBuilder.beginControlFlow("if (model.$N() == null)", field.name())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " must not be null")
            .endControlFlow();
        if (field.bitCount() == 0 && field.lowerBound() > 0) {
          methodBuilder.beginControlFlow(
                  "if (model.$N().length * 8 != $L)", field.name(), field.lowerBound())
              .addStatement("throw new $T($S)", IllegalArgumentException.class,
                  field.name() + " must be exactly " + field.lowerBound() + " bits")
              .endControlFlow();
        }
      } else if (field.encoding() == Encoding.OCTET_STRING) {
        methodBuilder.beginControlFlow("if (model.$N() == null)", field.name())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " must not be null")
            .endControlFlow();
        if (field.lowerBound() > 0) {
          methodBuilder.beginControlFlow("if (model.$N().length < $L)",
                  field.name(), field.lowerBound())
              .addStatement("throw new $T($S)", IllegalArgumentException.class,
                  field.name() + " length must be >= " + field.lowerBound())
              .endControlFlow();
        }
        if (field.upperBound() != Long.MAX_VALUE) {
          methodBuilder.beginControlFlow("if (model.$N().length > $L)",
                  field.name(), (int) field.upperBound())
              .addStatement("throw new $T($S)", IllegalArgumentException.class,
                  field.name() + " length must be <= " + (int) field.upperBound())
              .endControlFlow();
        }
      } else if (field.encoding() == Encoding.UNCONSTRAINED) {
        if (field.upperBound() != Long.MAX_VALUE) {
          methodBuilder.beginControlFlow("if (model.$N() > $LL)", field.name(), field.upperBound())
              .addStatement("throw new $T($S)", IllegalArgumentException.class,
                  field.name() + " must be <= " + field.upperBound())
              .endControlFlow();
        }
      } else if (field.encoding() != Encoding.BOOLEAN && field.encoding() != Encoding.TYPE_REFERENCE) {
        methodBuilder.beginControlFlow("if (model.$N() < $L)", field.name(), field.lowerBound())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " must be >= " + field.lowerBound())
            .endControlFlow();
      }
    }

    fields.forEach(field -> addEncodeStatement(methodBuilder, field, targetPackage));
    return methodBuilder.build();
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

    fields.forEach(field -> addDecodeStatement(methodBuilder, field, targetPackage));

    String args = fields.stream()
        .map(EncodedField::name)
        .collect(Collectors.joining(", "));
    methodBuilder.addStatement("return new $T($L)", modelClass, args);
    return methodBuilder.build();
  }

  private static void addEncodeStatement(MethodSpec.Builder builder, EncodedField field,
      String targetPackage) {
    switch (field.encoding()) {
      case SEMI_CONSTRAINED -> {
        if (field.lowerBound() == 0) {
          builder.addStatement("$T.encodeSemiConstrainedInt(out, model.$N())",
              UPER_CODEC_SUPPORT, field.name());
        } else {
          builder.addStatement("$T.encodeSemiConstrainedInt(out, model.$N() - $L)",
              UPER_CODEC_SUPPORT, field.name(), field.lowerBound());
        }
      }
      case CONSTRAINED -> {
        if (field.lowerBound() == 0) {
          builder.addStatement("out.writeBits(model.$N(), $L)", field.name(), field.bitCount());
        } else {
          builder.addStatement("out.writeBits(model.$N() - $L, $L)",
              field.name(), field.lowerBound(), field.bitCount());
        }
      }
      case ZERO_RANGE -> { /* nothing to encode */ }
      case UNCONSTRAINED -> builder.addStatement("$T.encodeUnconstrainedInt(out, model.$N())",
          UPER_CODEC_SUPPORT, field.name());
      case OCTET_STRING -> {
        if (field.bitCount() == 0) {
          builder.addStatement("$T.encodeFixedOctetString(out, model.$N())",
              UPER_CODEC_SUPPORT, field.name());
        } else {
          builder.addStatement("$T.encodeOctetString(out, model.$N(), $L, $L)",
              UPER_CODEC_SUPPORT, field.name(), field.lowerBound(), (int) field.upperBound());
        }
      }
      case BIT_STRING -> builder.addStatement("$T.encodeBitString(out, model.$N(), $L)",
          UPER_CODEC_SUPPORT, field.name(), field.lowerBound());
      case IA5_STRING -> builder.addStatement("$T.encodeIa5String(out, model.$N(), $L, $L)",
          UPER_CODEC_SUPPORT, field.name(), field.lowerBound(), (int) field.upperBound());
      case VISIBLE_STRING -> builder.addStatement("$T.encodeVisibleString(out, model.$N(), $L, $L)",
          UPER_CODEC_SUPPORT, field.name(), field.lowerBound(), (int) field.upperBound());
      case BOOLEAN -> builder.addStatement("out.writeBits(model.$N() ? 1 : 0, 1)", field.name());
      case UTF8_STRING -> builder.addStatement("$T.encodeUtf8String(out, model.$N())",
          UPER_CODEC_SUPPORT, field.name());
      case ENUMERATED -> builder.addStatement("out.writeBits(model.$N(), $L)",
          field.name(), field.bitCount());
      case TYPE_REFERENCE -> builder.addStatement("new $T().encodeInto(out, model.$N())",
          ClassName.get(targetPackage, field.referencedTypeName() + "Codec"), field.name());
    }
  }

  private static void addDecodeStatement(MethodSpec.Builder builder, EncodedField field,
      String targetPackage) {
    switch (field.encoding()) {
      case SEMI_CONSTRAINED -> {
        if (field.lowerBound() == 0) {
          builder.addStatement("int $N = (int) $T.decodeSemiConstrainedInt(in)", field.name(),
              UPER_CODEC_SUPPORT);
        } else {
          builder.addStatement("int $N = (int) $T.decodeSemiConstrainedInt(in) + $L", field.name(),
              UPER_CODEC_SUPPORT, field.lowerBound());
        }
      }
      case CONSTRAINED -> {
        if (field.lowerBound() == 0) {
          builder.addStatement("int $N = (int) in.readBits($L)", field.name(), field.bitCount());
        } else {
          builder.addStatement("int $N = (int) in.readBits($L) + $L", field.name(),
              field.bitCount(), field.lowerBound());
        }
      }
      case ZERO_RANGE -> builder.addStatement("int $N = $L", field.name(), field.lowerBound());
      case UNCONSTRAINED -> builder.addStatement("long $N = $T.decodeUnconstrainedInt(in)",
          field.name(), UPER_CODEC_SUPPORT);
      case OCTET_STRING -> {
        if (field.bitCount() == 0) {
          builder.addStatement("byte[] $N = $T.decodeFixedOctetString(in, $L)",
              field.name(), UPER_CODEC_SUPPORT, field.lowerBound());
        } else {
          builder.addStatement("byte[] $N = $T.decodeOctetString(in, $L, $L)",
              field.name(), UPER_CODEC_SUPPORT, field.lowerBound(), (int) field.upperBound());
        }
      }
      case BIT_STRING -> builder.addStatement("byte[] $N = $T.decodeBitString(in, $L)",
          field.name(), UPER_CODEC_SUPPORT, field.lowerBound());
      case IA5_STRING -> builder.addStatement("$T $N = $T.decodeIa5String(in, $L, $L)",
          ClassName.get("java.lang", "String"), field.name(), UPER_CODEC_SUPPORT,
          field.lowerBound(), (int) field.upperBound());
      case VISIBLE_STRING -> builder.addStatement("$T $N = $T.decodeVisibleString(in, $L, $L)",
          ClassName.get("java.lang", "String"), field.name(), UPER_CODEC_SUPPORT,
          field.lowerBound(), (int) field.upperBound());
      case BOOLEAN -> builder.addStatement("boolean $N = in.readBits(1) != 0", field.name());
      case UTF8_STRING -> builder.addStatement("$T $N = $T.decodeUtf8String(in)",
          ClassName.get("java.lang", "String"), field.name(), UPER_CODEC_SUPPORT);
      case ENUMERATED -> builder.addStatement("int $N = (int) in.readBits($L)",
          field.name(), field.bitCount());
      case TYPE_REFERENCE -> builder.addStatement("$T $N = new $T().decodeFrom(in)",
          ClassName.get(targetPackage, field.referencedTypeName()),
          field.name(),
          ClassName.get(targetPackage, field.referencedTypeName() + "Codec"));
    }
  }

  private static List<EncodedField> collectFields(TypeAssignmentNode typeAssignment) {
    return switch (typeAssignment.type()) {
      case SequenceTypeNode seq -> seq.fields()
          .stream()
          .filter(field -> !(field.type() instanceof NullTypeNode))
          .map(field -> {
            String javaName = CodegenUtils.toJavaFieldName(field.name());
            return switch (field.type()) {
              case IntegerTypeNode intType -> toEncodedField(javaName, intType);
              case BooleanTypeNode ignored -> new EncodedField(javaName, 0, Encoding.BOOLEAN, 1);
              case Utf8StringTypeNode utf8Type -> toEncodedField(javaName, utf8Type);
              case OctetStringTypeNode octetType -> toEncodedField(javaName, octetType);
              case BitStringTypeNode bitType -> toEncodedField(javaName, bitType);
              case NullTypeNode ignored ->
                  throw new IllegalStateException("null type should have been filtered");
              case Ia5StringTypeNode ia5Type -> toEncodedField(javaName, ia5Type);
              case VisibleStringTypeNode visibleType -> toEncodedField(javaName, visibleType);
              case SequenceTypeNode ignored ->
                  throw new IllegalArgumentException("nested SEQUENCE not supported");
              case EnumeratedTypeNode enumType ->
                  new EncodedField(javaName, 0, Encoding.ENUMERATED, enumBitCount(enumType));
              case TypeReferenceNode ref ->
                  new EncodedField(javaName, 0, Encoding.TYPE_REFERENCE, 0, Long.MAX_VALUE,
                      ref.typeName());
            };
          })
          .collect(Collectors.toList());
      case IntegerTypeNode intType -> List.of(toEncodedField("value", intType));
      case BooleanTypeNode ignored -> List.of(new EncodedField("value", 0, Encoding.BOOLEAN, 1));
      case Utf8StringTypeNode utf8Type -> List.of(toEncodedField("value", utf8Type));
      case OctetStringTypeNode octetType -> List.of(toEncodedField("value", octetType));
      case BitStringTypeNode bitType -> List.of(toEncodedField("value", bitType));
      case NullTypeNode ignored -> List.of();
      case Ia5StringTypeNode ia5Type -> List.of(toEncodedField("value", ia5Type));
      case VisibleStringTypeNode visibleType -> List.of(toEncodedField("value", visibleType));
      case EnumeratedTypeNode enumType ->
          List.of(new EncodedField("value", 0, Encoding.ENUMERATED, enumBitCount(enumType)));
      case TypeReferenceNode ignored ->
          throw new IllegalArgumentException(
              "top-level TypeReferenceNode is not a valid type assignment body");
    };
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

  private static int enumBitCount(EnumeratedTypeNode enumType) {
    int count = enumType.values().size();
    return count <= 1 ? 0 : Integer.SIZE - Integer.numberOfLeadingZeros(count - 1);
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
              yield new EncodedField(name, lb, Encoding.ZERO_RANGE, 0);
            }
            int bitCount = Integer.SIZE - Integer.numberOfLeadingZeros(range);
            yield new EncodedField(name, lb, Encoding.CONSTRAINED, bitCount);
          }
          case MinBound ignored -> throw new IllegalArgumentException(
              "upper bound cannot be MIN for field " + name);
        };
      }
      case MaxBound ignored -> throw new IllegalArgumentException(
          "lower bound cannot be MAX for field " + name);
    };
  }

  private enum Encoding {
    SEMI_CONSTRAINED, CONSTRAINED, ZERO_RANGE, BOOLEAN, UTF8_STRING, ENUMERATED, UNCONSTRAINED,
    OCTET_STRING, BIT_STRING, IA5_STRING, VISIBLE_STRING, TYPE_REFERENCE
  }

  private record EncodedField(String name, int lowerBound, Encoding encoding, int bitCount,
      long upperBound, String referencedTypeName) {
    EncodedField(String name, int lowerBound, Encoding encoding, int bitCount) {
      this(name, lowerBound, encoding, bitCount, Long.MAX_VALUE, null);
    }

    EncodedField(String name, int lowerBound, Encoding encoding, int bitCount, long upperBound) {
      this(name, lowerBound, encoding, bitCount, upperBound, null);
    }
  }
}
