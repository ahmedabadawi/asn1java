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
import io.github.ahmedabadawi.asn1java.core.ast.FieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MaxBound;
import io.github.ahmedabadawi.asn1java.core.ast.NumberBound;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class CodecGenerator {

    private static final String RUNTIME_PKG = "io.github.ahmedabadawi.asn1java.runtime.uper";

    private static final ClassName UPER_OUTPUT_STREAM = ClassName.get(RUNTIME_PKG, "UperOutputStream");
    private static final ClassName UPER_INPUT_STREAM = ClassName.get(RUNTIME_PKG, "UperInputStream");
    private static final ClassName UPER_CODEC_SUPPORT = ClassName.get(RUNTIME_PKG, "UperCodecSupport");

    private CodecGenerator() {}

    static JavaFile generate(String pkg, TypeAssignmentNode ta) {
        ClassName modelClass = ClassName.get(pkg, ta.name());
        List<EncodedField> fields = collectFields(ta);

        TypeSpec codec = TypeSpec.classBuilder(ta.name() + "Codec")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(buildEncodeMethod(modelClass, fields))
                .addMethod(buildDecodeMethod(modelClass, fields))
                .build();
        return JavaFile.builder(pkg, codec).build();
    }

    private static MethodSpec buildEncodeMethod(ClassName modelClass, List<EncodedField> fields) {
        MethodSpec.Builder m = MethodSpec.methodBuilder("encode")
                .addModifiers(Modifier.PUBLIC)
                .returns(ArrayTypeName.of(TypeName.BYTE))
                .addParameter(ParameterSpec.builder(modelClass, "model").build());

        // Validation
        for (EncodedField f : fields) {
            if (f.encoding() != Encoding.BOOLEAN) {
                m.beginControlFlow("if (model.$N() < $L)", f.name(), f.lowerBound())
                        .addStatement("throw new $T($S)", IllegalArgumentException.class,
                                f.name() + " must be >= " + f.lowerBound())
                        .endControlFlow();
            }
        }

        m.addStatement("$T out = new $T()", UPER_OUTPUT_STREAM, UPER_OUTPUT_STREAM);

        for (EncodedField f : fields) {
            addEncodeStatement(m, f);
        }

        m.addStatement("return out.toByteArray()");
        return m.build();
    }

    private static MethodSpec buildDecodeMethod(ClassName modelClass, List<EncodedField> fields) {
        MethodSpec.Builder m = MethodSpec.methodBuilder("decode")
                .addModifiers(Modifier.PUBLIC)
                .returns(modelClass)
                .addParameter(ParameterSpec.builder(ArrayTypeName.of(TypeName.BYTE), "data").build());

        m.addStatement("$T in = new $T(data)", UPER_INPUT_STREAM, UPER_INPUT_STREAM);

        for (EncodedField f : fields) {
            addDecodeStatement(m, f);
        }

        String args = fields.stream().map(EncodedField::name).collect(Collectors.joining(", "));
        m.addStatement("return new $T($L)", modelClass, args);
        return m.build();
    }

    private static void addEncodeStatement(MethodSpec.Builder m, EncodedField f) {
        switch (f.encoding()) {
            case SEMI_CONSTRAINED -> {
                if (f.lowerBound() == 0) {
                    m.addStatement("$T.encodeSemiConstrainedInt(out, model.$N())",
                            UPER_CODEC_SUPPORT, f.name());
                } else {
                    m.addStatement("$T.encodeSemiConstrainedInt(out, model.$N() - $L)",
                            UPER_CODEC_SUPPORT, f.name(), f.lowerBound());
                }
            }
            case CONSTRAINED -> {
                if (f.lowerBound() == 0) {
                    m.addStatement("out.writeBits(model.$N(), $L)", f.name(), f.bitCount());
                } else {
                    m.addStatement("out.writeBits(model.$N() - $L, $L)",
                            f.name(), f.lowerBound(), f.bitCount());
                }
            }
            case ZERO_RANGE -> { /* nothing to encode */ }
            case BOOLEAN -> m.addStatement("out.writeBits(model.$N() ? 1 : 0, 1)", f.name());
        }
    }

    private static void addDecodeStatement(MethodSpec.Builder m, EncodedField f) {
        switch (f.encoding()) {
            case SEMI_CONSTRAINED -> {
                if (f.lowerBound() == 0) {
                    m.addStatement("int $N = (int) $T.decodeSemiConstrainedInt(in)",
                            f.name(), UPER_CODEC_SUPPORT);
                } else {
                    m.addStatement("int $N = (int) $T.decodeSemiConstrainedInt(in) + $L",
                            f.name(), UPER_CODEC_SUPPORT, f.lowerBound());
                }
            }
            case CONSTRAINED -> {
                if (f.lowerBound() == 0) {
                    m.addStatement("int $N = (int) in.readBits($L)", f.name(), f.bitCount());
                } else {
                    m.addStatement("int $N = (int) in.readBits($L) + $L",
                            f.name(), f.bitCount(), f.lowerBound());
                }
            }
            case ZERO_RANGE -> m.addStatement("int $N = $L", f.name(), f.lowerBound());
            case BOOLEAN -> m.addStatement("boolean $N = in.readBits(1) != 0", f.name());
        }
    }

    private static List<EncodedField> collectFields(TypeAssignmentNode ta) {
        return switch (ta.type()) {
            case SequenceTypeNode seq -> seq.fields().stream()
                    .map(f -> switch (f.type()) {
                        case IntegerTypeNode intType -> toEncodedField(f.name(), intType);
                        case BooleanTypeNode ignored -> new EncodedField(f.name(), 0, Encoding.BOOLEAN, 1);
                        case SequenceTypeNode ignored -> throw new IllegalArgumentException(
                                "nested SEQUENCE not supported");
                    })
                    .collect(Collectors.toList());
            case IntegerTypeNode intType -> List.of(toEncodedField("value", intType));
            case BooleanTypeNode ignored -> List.of(new EncodedField("value", 0, Encoding.BOOLEAN, 1));
        };
    }

    private static EncodedField toEncodedField(String name, IntegerTypeNode intType) {
        ConstraintNode c = intType.constraint();
        if (c == null) {
            return new EncodedField(name, 0, Encoding.SEMI_CONSTRAINED, 0);
        }
        int lb = c.lowerBound();
        return switch (c.upperBound()) {
            case MaxBound ignored -> new EncodedField(name, lb, Encoding.SEMI_CONSTRAINED, 0);
            case NumberBound nb -> {
                int range = nb.value() - lb;
                if (range == 0) {
                    yield new EncodedField(name, lb, Encoding.ZERO_RANGE, 0);
                }
                int bitCount = Integer.SIZE - Integer.numberOfLeadingZeros(range);
                yield new EncodedField(name, lb, Encoding.CONSTRAINED, bitCount);
            }
        };
    }

    private enum Encoding { SEMI_CONSTRAINED, CONSTRAINED, ZERO_RANGE, BOOLEAN }

    private record EncodedField(String name, int lowerBound, Encoding encoding, int bitCount) {}
}
