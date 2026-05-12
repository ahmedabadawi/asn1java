package io.github.ahmedabadawi.asn1java.core.codegen;

import com.palantir.javapoet.JavaFile;
import io.github.ahmedabadawi.asn1java.core.ast.BooleanTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ConstraintNode;
import io.github.ahmedabadawi.asn1java.core.ast.FieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MaxBound;
import io.github.ahmedabadawi.asn1java.core.ast.ModuleNode;
import io.github.ahmedabadawi.asn1java.core.ast.NumberBound;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Asn1CodeGeneratorTest {

    private static ModuleNode versionInfoModule() {
        return new ModuleNode("VersionInfo", List.of(
                new TypeAssignmentNode("Version", new SequenceTypeNode(List.of(
                        new FieldNode("major", new IntegerTypeNode(new ConstraintNode(0, new MaxBound()))),
                        new FieldNode("minor", new IntegerTypeNode(new ConstraintNode(0, new MaxBound())))
                )))
        ));
    }

    private static JavaFile findFile(List<JavaFile> files, String typeName) {
        return files.stream()
                .filter(f -> f.toString().contains(typeName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No file found for type: " + typeName));
    }

    @Test
    void generate_simpleSequence_producesTwoFiles() {
        var files = new Asn1CodeGenerator("io.example").generate(versionInfoModule());
        assertThat(files).hasSize(2);
    }

    @Test
    void generate_packageIncludesModuleNameLowercase() {
        var files = new Asn1CodeGenerator("io.example").generate(versionInfoModule());
        assertThat(files).allSatisfy(f ->
                assertThat(f.packageName()).isEqualTo("io.example.versioninfo")
        );
    }

    @Test
    void generate_modelFile_containsRecordWithFields() {
        var files = new Asn1CodeGenerator("io.example").generate(versionInfoModule());
        String model = findFile(files, "record Version").toString();
        assertThat(model).contains("public record Version(");
        assertThat(model).contains("int major");
        assertThat(model).contains("int minor");
    }

    @Test
    void generate_codecFile_containsEncodeAndDecode() {
        var files = new Asn1CodeGenerator("io.example").generate(versionInfoModule());
        String codec = findFile(files, "VersionCodec").toString();
        assertThat(codec).contains("public byte[] encode(");
        assertThat(codec).contains("public Version decode(");
    }

    @Test
    void generate_codecFile_usesSemiConstrainedForMaxBound() {
        var files = new Asn1CodeGenerator("io.example").generate(versionInfoModule());
        String codec = findFile(files, "VersionCodec").toString();
        assertThat(codec).contains("UperCodecSupport.encodeSemiConstrainedInt");
        assertThat(codec).contains("UperCodecSupport.decodeSemiConstrainedInt");
    }

    @Test
    void generate_codecFile_containsLowerBoundValidation() {
        var files = new Asn1CodeGenerator("io.example").generate(versionInfoModule());
        String codec = findFile(files, "VersionCodec").toString();
        assertThat(codec).contains("< 0");
        assertThat(codec).contains("IllegalArgumentException");
    }

    @Test
    void generate_constrainedField_emitsWriteBitsWithBitCount() {
        // INTEGER (0..255): range=255, bitCount = 32 - numberOfLeadingZeros(255) = 8
        var module = new ModuleNode("Flags", List.of(
                new TypeAssignmentNode("Flags", new SequenceTypeNode(List.of(
                        new FieldNode("value", new IntegerTypeNode(new ConstraintNode(0, new NumberBound(255))))
                )))
        ));
        var files = new Asn1CodeGenerator("io.example").generate(module);
        String codec = findFile(files, "FlagsCodec").toString();
        assertThat(codec).contains("writeBits");
        assertThat(codec).contains(", 8)");
    }

    @Test
    void generate_topLevelIntegerType_producesWrapperRecord() {
        var module = new ModuleNode("Types", List.of(
                new TypeAssignmentNode("MyInt", new IntegerTypeNode(new ConstraintNode(0, new NumberBound(255))))
        ));
        var files = new Asn1CodeGenerator("io.example").generate(module);
        String model = findFile(files, "record MyInt").toString();
        assertThat(model).contains("public record MyInt(");
        assertThat(model).contains("int value");
    }

    @Test
    void generate_topLevelIntegerType_producesCodec() {
        var module = new ModuleNode("Types", List.of(
                new TypeAssignmentNode("MyInt", new IntegerTypeNode(new ConstraintNode(0, new NumberBound(255))))
        ));
        var files = new Asn1CodeGenerator("io.example").generate(module);
        String codec = findFile(files, "MyIntCodec").toString();
        assertThat(codec).contains("public byte[] encode(");
        assertThat(codec).contains("public MyInt decode(");
    }

    @Test
    void generate_booleanFieldInSequence_producesJavaBooleanType() {
        var module = new ModuleNode("DeviceInfo", List.of(
                new TypeAssignmentNode("Device", new SequenceTypeNode(List.of(
                        new FieldNode("active", new BooleanTypeNode())
                )))
        ));
        var files = new Asn1CodeGenerator("io.example").generate(module);
        String model = findFile(files, "record Device").toString();
        assertThat(model).contains("boolean active");
    }

    @Test
    void generate_booleanField_emitsSingleBitWriteAndRead() {
        var module = new ModuleNode("DeviceInfo", List.of(
                new TypeAssignmentNode("Device", new SequenceTypeNode(List.of(
                        new FieldNode("active", new BooleanTypeNode())
                )))
        ));
        var files = new Asn1CodeGenerator("io.example").generate(module);
        String codec = findFile(files, "DeviceCodec").toString();
        assertThat(codec).contains("? 1 : 0, 1)");
        assertThat(codec).contains("readBits(1) != 0");
    }

    @Test
    void generate_topLevelBooleanType_producesBooleanRecord() {
        var module = new ModuleNode("Types", List.of(
                new TypeAssignmentNode("Flag", new BooleanTypeNode())
        ));
        var files = new Asn1CodeGenerator("io.example").generate(module);
        String model = findFile(files, "record Flag").toString();
        assertThat(model).contains("boolean value");
    }

    @Test
    void generate_nonZeroLowerBound_emitsOffsetInCode() {
        // INTEGER (10..20): lb=10, range=10, bitCount=4
        var module = new ModuleNode("M", List.of(
                new TypeAssignmentNode("Foo", new SequenceTypeNode(List.of(
                        new FieldNode("x", new IntegerTypeNode(new ConstraintNode(10, new NumberBound(20))))
                )))
        ));
        var files = new Asn1CodeGenerator("io.example").generate(module);
        String codec = findFile(files, "FooCodec").toString();
        assertThat(codec).contains("- 10");
        assertThat(codec).contains("+ 10");
    }
}
