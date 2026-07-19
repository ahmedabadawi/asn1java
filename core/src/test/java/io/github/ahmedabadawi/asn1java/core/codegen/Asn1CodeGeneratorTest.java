package io.github.ahmedabadawi.asn1java.core.codegen;

import com.palantir.javapoet.JavaFile;
import io.github.ahmedabadawi.asn1java.core.ast.BooleanTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ChoiceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ConstraintNode;
import io.github.ahmedabadawi.asn1java.core.ast.EnumeratedTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.FieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MaxBound;
import io.github.ahmedabadawi.asn1java.core.ast.MinBound;
import io.github.ahmedabadawi.asn1java.core.ast.ModuleNode;
import io.github.ahmedabadawi.asn1java.core.ast.NullTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.NumberBound;
import java.util.Optional;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeReferenceNode;
import io.github.ahmedabadawi.asn1java.core.ast.Utf8StringTypeNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Asn1CodeGeneratorTest {

  private static ModuleNode versionInfoModule() {
    return new ModuleNode("VersionInfo", List.of(new TypeAssignmentNode("Version",
        new SequenceTypeNode(List.of(
            new FieldNode("major", new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new MaxBound()))),
            new FieldNode("minor", new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new MaxBound()))))))));
  }

  private static JavaFile findFile(List<JavaFile> files, String typeName) {
    return files.stream().filter(f -> f.toString().contains(typeName)).findFirst()
        .orElseThrow(() -> new AssertionError("No file found for type: " + typeName));
  }

  @Test
  void generate_simpleSequence_producesTwoFiles() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(versionInfoModule());
    assertThat(files).hasSize(2);
  }

  @Test
  void generate_packageIncludesModuleNameLowercase() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(versionInfoModule());
    assertThat(files).allSatisfy(
        f -> assertThat(f.packageName()).isEqualTo("io.example.versioninfo"));
  }

  @Test
  void generate_modelFile_containsRecordWithFields() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(versionInfoModule());
    String model = findFile(files, "record Version").toString();
    assertThat(model).contains("public record Version(");
    assertThat(model).contains("int major");
    assertThat(model).contains("int minor");
  }

  @Test
  void generate_codecFile_containsEncodeAndDecode() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(versionInfoModule());
    String codec = findFile(files, "VersionCodec").toString();
    assertThat(codec).contains("public byte[] encode(");
    assertThat(codec).contains("public Version decode(");
  }

  @Test
  void generate_codecFile_usesSemiConstrainedForMaxBound() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(versionInfoModule());
    String codec = findFile(files, "VersionCodec").toString();
    assertThat(codec).contains("UperCodecSupport.encodeSemiConstrainedInt");
    assertThat(codec).contains("UperCodecSupport.decodeSemiConstrainedInt");
  }

  @Test
  void generate_modelFile_containsLowerBoundValidation() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(versionInfoModule());
    String model = findFile(files, "record Version").toString();
    assertThat(model).contains("< 0");
    assertThat(model).contains("IllegalArgumentException");
  }

  @Test
  void generate_constrainedField_emitsWriteBitsWithBitCount() {
    // INTEGER (0..255): range=255, bitCount = 32 - numberOfLeadingZeros(255) = 8
    var module = new ModuleNode("Flags", List.of(new TypeAssignmentNode("Flags",
        new SequenceTypeNode(List.of(new FieldNode("value",
            new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new NumberBound(255)))))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String codec = findFile(files, "FlagsCodec").toString();
    assertThat(codec).contains("writeBits");
    assertThat(codec).contains(", 8)");
  }

  @Test
  void generate_topLevelIntegerType_producesWrapperRecord() {
    var module = new ModuleNode("Types", List.of(new TypeAssignmentNode("MyInt",
        new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new NumberBound(255))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record MyInt").toString();
    assertThat(model).contains("public record MyInt(");
    assertThat(model).contains("int value");
  }

  @Test
  void generate_topLevelIntegerType_producesCodec() {
    var module = new ModuleNode("Types", List.of(new TypeAssignmentNode("MyInt",
        new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new NumberBound(255))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String codec = findFile(files, "MyIntCodec").toString();
    assertThat(codec).contains("public byte[] encode(");
    assertThat(codec).contains("public MyInt decode(");
  }

  @Test
  void generate_booleanFieldInSequence_producesJavaBooleanType() {
    var module = new ModuleNode("DeviceInfo", List.of(new TypeAssignmentNode("Device",
        new SequenceTypeNode(List.of(new FieldNode("active", new BooleanTypeNode()))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Device").toString();
    assertThat(model).contains("boolean active");
  }

  @Test
  void generate_booleanField_emitsSingleBitWriteAndRead() {
    var module = new ModuleNode("DeviceInfo", List.of(new TypeAssignmentNode("Device",
        new SequenceTypeNode(List.of(new FieldNode("active", new BooleanTypeNode()))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String codec = findFile(files, "DeviceCodec").toString();
    assertThat(codec).contains("? 1 : 0, 1)");
    assertThat(codec).contains("readBits(1) != 0");
  }

  @Test
  void generate_topLevelBooleanType_producesBooleanRecord() {
    var module =
        new ModuleNode("Types", List.of(new TypeAssignmentNode("Flag", new BooleanTypeNode())));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Flag").toString();
    assertThat(model).contains("boolean value");
  }

  @Test
  void generate_nonZeroLowerBound_emitsOffsetInCode() {
    // INTEGER (10..20): lb=10, range=10, bitCount=4
    var module = new ModuleNode("M", List.of(new TypeAssignmentNode("Foo", new SequenceTypeNode(
        List.of(new FieldNode("x",
            new IntegerTypeNode(new ConstraintNode(new NumberBound(10), new NumberBound(20)))))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String codec = findFile(files, "FooCodec").toString();
    assertThat(codec).contains("- 10");
    assertThat(codec).contains("+ 10");
  }

  @Test
  void generate_utf8StringFieldInSequence_producesJavaStringType() {
    var module = new ModuleNode("PersonInfo", List.of(new TypeAssignmentNode("Person",
        new SequenceTypeNode(List.of(new FieldNode("name", new Utf8StringTypeNode(Optional.empty())))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Person").toString();
    assertThat(model).contains("String name");
  }

  @Test
  void generate_utf8StringField_emitsUtf8StringHelperCalls() {
    var module = new ModuleNode("PersonInfo", List.of(new TypeAssignmentNode("Person",
        new SequenceTypeNode(List.of(new FieldNode("name", new Utf8StringTypeNode(Optional.empty())))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String codec = findFile(files, "PersonCodec").toString();
    assertThat(codec).contains("encodeUtf8String");
    assertThat(codec).contains("decodeUtf8String");
  }

  @Test
  void generate_utf8StringField_emitsNullValidation() {
    var module = new ModuleNode("PersonInfo", List.of(new TypeAssignmentNode("Person",
        new SequenceTypeNode(List.of(new FieldNode("name", new Utf8StringTypeNode(Optional.empty())))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Person").toString();
    assertThat(model).contains("== null");
    assertThat(model).contains("must not be null");
  }

  @Test
  void generate_enumeratedFieldInSequence_producesIntField() {
    // Status ::= SEQUENCE { state ENUMERATED { pending, active, inactive } }
    var module = new ModuleNode("StatusInfo", List.of(new TypeAssignmentNode("Status",
        new SequenceTypeNode(List.of(
            new FieldNode("state", new EnumeratedTypeNode(List.of("pending", "active", "inactive"))))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Status").toString();
    assertThat(model).contains("int state");
  }

  @Test
  void generate_enumeratedFieldInSequence_emitsWriteBitsWithBitCount() {
    // 3 values: range=2, bitCount=2
    var module = new ModuleNode("StatusInfo", List.of(new TypeAssignmentNode("Status",
        new SequenceTypeNode(List.of(
            new FieldNode("state", new EnumeratedTypeNode(List.of("pending", "active", "inactive"))))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String codec = findFile(files, "StatusCodec").toString();
    assertThat(codec).contains("writeBits(model.state(), 2)");
    assertThat(codec).contains("readBits(2)");
  }

  @Test
  void generate_enumeratedFieldInSequence_emitsLowerBoundValidation() {
    var module = new ModuleNode("StatusInfo", List.of(new TypeAssignmentNode("Status",
        new SequenceTypeNode(List.of(
            new FieldNode("state", new EnumeratedTypeNode(List.of("pending", "active", "inactive"))))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Status").toString();
    assertThat(model).contains("< 0");
    assertThat(model).contains("IllegalArgumentException");
  }

  @Test
  void generate_enumeratedFieldInSequence_emitsUpperBoundValidation() {
    // 3 values: valid indexes are 0..2, so upper bound is 2
    var module = new ModuleNode("StatusInfo", List.of(new TypeAssignmentNode("Status",
        new SequenceTypeNode(List.of(
            new FieldNode("state", new EnumeratedTypeNode(List.of("pending", "active", "inactive"))))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Status").toString();
    assertThat(model).contains("> 2");
    assertThat(model).contains("must be <= 2");
  }

  @Test
  void generate_topLevelEnumeratedType_producesIntWrapperRecord() {
    var module = new ModuleNode("Types", List.of(new TypeAssignmentNode("State",
        new EnumeratedTypeNode(List.of("on", "off")))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record State").toString();
    assertThat(model).contains("int value");
  }

  @Test
  void generate_enumeratedBitCount_twoValues_produces1bit() {
    // 2 values: range=1, bitCount=1
    var module = new ModuleNode("Types", List.of(new TypeAssignmentNode("Switch",
        new SequenceTypeNode(List.of(
            new FieldNode("state", new EnumeratedTypeNode(List.of("on", "off"))))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String codec = findFile(files, "SwitchCodec").toString();
    assertThat(codec).contains("writeBits(model.state(), 1)");
  }

  @Test
  void generate_minBoundedField_producesLongTypeAndUnconstrainedEncoding() {
    // INTEGER (MIN..0): lower=MIN, upper=0 — unconstrained whole number (X.691 §12.2.3)
    var module = new ModuleNode("M", List.of(new TypeAssignmentNode("Offset", new SequenceTypeNode(
        List.of(new FieldNode("delta",
            new IntegerTypeNode(new ConstraintNode(new MinBound(), new NumberBound(0)))))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Offset").toString();
    String codec = findFile(files, "OffsetCodec").toString();
    assertThat(model).contains("long delta");
    assertThat(codec).contains("encodeUnconstrainedInt");
    assertThat(codec).contains("decodeUnconstrainedInt");
  }

  @Test
  void generate_minBoundedField_emitsUpperBoundValidation() {
    var module = new ModuleNode("M", List.of(new TypeAssignmentNode("Offset", new SequenceTypeNode(
        List.of(new FieldNode("delta",
            new IntegerTypeNode(new ConstraintNode(new MinBound(), new NumberBound(0)))))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Offset").toString();
    assertThat(model).contains("> 0");
    assertThat(model).contains("must be <= 0");
  }

  private static ModuleNode propulsionModule() {
    return new ModuleNode("VehicleModule", List.of(
        new TypeAssignmentNode("GasEngine", new SequenceTypeNode(List.of(
            new FieldNode("displacementCc",
                new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new NumberBound(8000)))),
            new FieldNode("cylinders",
                new IntegerTypeNode(new ConstraintNode(new NumberBound(1), new NumberBound(16))))))),
        new TypeAssignmentNode("Propulsion", new ChoiceTypeNode(List.of(
            new FieldNode("gasoline", new TypeReferenceNode("GasEngine")),
            new FieldNode("none", new NullTypeNode()))))));
  }

  @Test
  void generate_choiceType_producesSealedInterfaceWithNestedRecords() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(propulsionModule());
    String model = findFile(files, "sealed interface Propulsion").toString();
    assertThat(model).contains("public sealed interface Propulsion");
    assertThat(model).contains("record Gasoline(");
    assertThat(model).contains("record None(");
  }

  @Test
  void generate_choiceType_emitsIndexWriteAndReadWithBitCount() {
    // 2 alternatives: range=1, bitCount=1
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(propulsionModule());
    String codec = findFile(files, "PropulsionCodec").toString();
    assertThat(codec).contains("writeBits(0, 1)");
    assertThat(codec).contains("writeBits(1, 1)");
    assertThat(codec).contains("readBits(1)");
  }

  @Test
  void generate_choiceType_delegatesToAlternativeCodec() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(propulsionModule());
    String codec = findFile(files, "PropulsionCodec").toString();
    assertThat(codec).contains("GasEngineCodec().encodeInto(out, variant.value())");
    assertThat(codec).contains("new GasEngineCodec().decodeFrom(in)");
  }

  @Test
  void generate_choiceTypeReferenceAlternative_emitsNullValidation() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(propulsionModule());
    String model = findFile(files, "record Gasoline").toString();
    assertThat(model).contains("== null");
    assertThat(model).contains("value must not be null");
  }

  private static ModuleNode nestedReferenceModule() {
    return new ModuleNode("ContainerModule", List.of(
        new TypeAssignmentNode("Inner", new SequenceTypeNode(List.of(
            new FieldNode("value",
                new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new NumberBound(255))))))),
        new TypeAssignmentNode("Outer", new SequenceTypeNode(List.of(
            new FieldNode("inner", new TypeReferenceNode("Inner")))))));
  }

  @Test
  void generate_typeReferenceFieldInSequence_emitsNullValidation() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(nestedReferenceModule());
    String model = findFile(files, "record Outer").toString();
    assertThat(model).contains("== null");
    assertThat(model).contains("inner must not be null");
  }
}
