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
import io.github.ahmedabadawi.asn1java.core.ast.MaxBound;
import io.github.ahmedabadawi.asn1java.core.ast.MinBound;
import io.github.ahmedabadawi.asn1java.core.ast.NumberBound;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;
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
            .addMethod(buildEncodeMethod(modelClass, fields))
            .addMethod(buildDecodeMethod(modelClass, fields)).build();
    return JavaFile.builder(targetPackage, codec).build();
  }

  private static MethodSpec buildEncodeMethod(ClassName modelClass, List<EncodedField> fields) {
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("encode")
        .addModifiers(Modifier.PUBLIC)
        .returns(ArrayTypeName.of(TypeName.BYTE))
        .addParameter(ParameterSpec.builder(modelClass, "model").build());

    // Validation
    for (EncodedField field : fields) {
      if (field.encoding() == Encoding.UTF8_STRING) {
        methodBuilder.beginControlFlow("if (model.$N() == null)", field.name())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " must not be null")
            .endControlFlow();
      } else if (field.encoding() == Encoding.UNCONSTRAINED) {
        if (field.upperBound() != Long.MAX_VALUE) {
          methodBuilder.beginControlFlow("if (model.$N() > $LL)", field.name(), field.upperBound())
              .addStatement("throw new $T($S)", IllegalArgumentException.class,
                  field.name() + " must be <= " + field.upperBound())
              .endControlFlow();
        }
      } else if (field.encoding() != Encoding.BOOLEAN) {
        methodBuilder.beginControlFlow("if (model.$N() < $L)",
                field.name(), field.lowerBound())
            .addStatement("throw new $T($S)", IllegalArgumentException.class,
                field.name() + " must be >= " + field.lowerBound())
            .endControlFlow();
      }
    }

    methodBuilder.addStatement("$T out = new $T()", UPER_OUTPUT_STREAM, UPER_OUTPUT_STREAM);

    fields.forEach(field -> addEncodeStatement(methodBuilder, field));

    methodBuilder.addStatement("return out.toByteArray()");
    return methodBuilder.build();
  }

  private static MethodSpec buildDecodeMethod(ClassName modelClass, List<EncodedField> fields) {
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("decode")
        .addModifiers(Modifier.PUBLIC)
        .returns(modelClass)
        .addParameter(
            ParameterSpec.builder(ArrayTypeName.of(TypeName.BYTE), "data").build());

    methodBuilder.addStatement("$T in = new $T(data)", UPER_INPUT_STREAM, UPER_INPUT_STREAM);

    fields.forEach(field -> addDecodeStatement(methodBuilder, field));

    String args = fields.stream()
        .map(EncodedField::name)
        .collect(Collectors.joining(", "));
    methodBuilder.addStatement("return new $T($L)", modelClass, args);
    return methodBuilder.build();
  }

  private static void addEncodeStatement(MethodSpec.Builder builder, EncodedField field) {
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
          builder.addStatement("out.writeBits(model.$N(), $L)",
              field.name(), field.bitCount());
        } else {
          builder.addStatement("out.writeBits(model.$N() - $L, $L)",
              field.name(), field.lowerBound(), field.bitCount());
        }
      }
      case ZERO_RANGE -> { /* nothing to encode */ }
      case UNCONSTRAINED -> builder.addStatement("$T.encodeUnconstrainedInt(out, model.$N())",
          UPER_CODEC_SUPPORT, field.name());
      case BOOLEAN -> builder.addStatement("out.writeBits(model.$N() ? 1 : 0, 1)", field.name());
      case UTF8_STRING -> builder.addStatement("$T.encodeUtf8String(out, model.$N())",
              UPER_CODEC_SUPPORT, field.name());
      case ENUMERATED -> builder.addStatement("out.writeBits(model.$N(), $L)",
              field.name(), field.bitCount());
    }
  }

  private static void addDecodeStatement(MethodSpec.Builder builder, EncodedField field) {
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
          builder.addStatement("int $N = (int) in.readBits($L) + $L", field.name(), field.bitCount(),
              field.lowerBound());
        }
      }
      case ZERO_RANGE -> builder.addStatement("int $N = $L", field.name(), field.lowerBound());
      case UNCONSTRAINED -> builder.addStatement("long $N = $T.decodeUnconstrainedInt(in)",
          field.name(), UPER_CODEC_SUPPORT);
      case BOOLEAN -> builder.addStatement("boolean $N = in.readBits(1) != 0", field.name());
      case UTF8_STRING ->
          builder.addStatement("$T $N = $T.decodeUtf8String(in)", ClassName.get("java.lang", "String"),
              field.name(), UPER_CODEC_SUPPORT);
      case ENUMERATED -> builder.addStatement("int $N = (int) in.readBits($L)",
              field.name(), field.bitCount());
    }
  }

  private static List<EncodedField> collectFields(TypeAssignmentNode typeAssignment) {
    return switch (typeAssignment.type()) {
      case SequenceTypeNode seq -> seq.fields()
          .stream()
          .map(field -> switch (field.type()) {
            case IntegerTypeNode intType -> toEncodedField(field.name(), intType);
            case BooleanTypeNode ignored -> new EncodedField(field.name(), 0, Encoding.BOOLEAN, 1);
            case Utf8StringTypeNode ignored -> new EncodedField(field.name(), 0, Encoding.UTF8_STRING, 0);
            case SequenceTypeNode ignored ->
                throw new IllegalArgumentException("nested SEQUENCE not supported");
            case EnumeratedTypeNode enumType ->
                new EncodedField(field.name(), 0, Encoding.ENUMERATED, enumBitCount(enumType));
          })
          .collect(Collectors.toList());
      case IntegerTypeNode intType -> List.of(toEncodedField("value", intType));
      case BooleanTypeNode ignored -> List.of(new EncodedField("value", 0, Encoding.BOOLEAN, 1));
      case Utf8StringTypeNode ignored ->
          List.of(new EncodedField("value", 0, Encoding.UTF8_STRING, 0));
      case EnumeratedTypeNode enumType ->
          List.of(new EncodedField("value", 0, Encoding.ENUMERATED, enumBitCount(enumType)));
    };
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

  private enum Encoding {SEMI_CONSTRAINED, CONSTRAINED, ZERO_RANGE, BOOLEAN, UTF8_STRING, ENUMERATED, UNCONSTRAINED}

  private record EncodedField(String name, int lowerBound, Encoding encoding, int bitCount, long upperBound) {
    EncodedField(String name, int lowerBound, Encoding encoding, int bitCount) {
      this(name, lowerBound, encoding, bitCount, Long.MAX_VALUE);
    }
  }
}
