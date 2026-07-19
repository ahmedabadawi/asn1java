package io.github.ahmedabadawi.asn1java.core.codegen;

import com.palantir.javapoet.JavaFile;
import io.github.ahmedabadawi.asn1java.core.ast.BooleanDefaultValueNode;
import io.github.ahmedabadawi.asn1java.core.ast.BooleanTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ChoiceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ConstraintNode;
import io.github.ahmedabadawi.asn1java.core.ast.EnumeratedDefaultValueNode;
import io.github.ahmedabadawi.asn1java.core.ast.EnumeratedTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.FieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerDefaultValueNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MaxBound;
import io.github.ahmedabadawi.asn1java.core.ast.MinBound;
import io.github.ahmedabadawi.asn1java.core.ast.ModuleNode;
import io.github.ahmedabadawi.asn1java.core.ast.NullTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.NumberBound;
import java.util.Optional;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceFieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.StringDefaultValueNode;
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
            new SequenceFieldNode("major",
                new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new MaxBound())), false, null),
            new SequenceFieldNode("minor",
                new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new MaxBound())), false, null))))));
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
        new SequenceTypeNode(List.of(new SequenceFieldNode("value",
            new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new NumberBound(255))),
            false, null))))));
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
        new SequenceTypeNode(List.of(new SequenceFieldNode("active", new BooleanTypeNode(), false, null))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Device").toString();
    assertThat(model).contains("boolean active");
  }

  @Test
  void generate_booleanField_emitsSingleBitWriteAndRead() {
    var module = new ModuleNode("DeviceInfo", List.of(new TypeAssignmentNode("Device",
        new SequenceTypeNode(List.of(new SequenceFieldNode("active", new BooleanTypeNode(), false, null))))));
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
        List.of(new SequenceFieldNode("x",
            new IntegerTypeNode(new ConstraintNode(new NumberBound(10), new NumberBound(20))),
            false, null))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String codec = findFile(files, "FooCodec").toString();
    assertThat(codec).contains("- 10");
    assertThat(codec).contains("+ 10");
  }

  @Test
  void generate_zeroRangeField_emitsEqualityValidation() {
    // INTEGER (5..5): lb=ub=5, nothing is written on the wire but the value must equal 5
    var module = new ModuleNode("M", List.of(new TypeAssignmentNode("Foo", new SequenceTypeNode(
        List.of(new SequenceFieldNode("x",
            new IntegerTypeNode(new ConstraintNode(new NumberBound(5), new NumberBound(5))),
            false, null))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Foo").toString();
    assertThat(model).contains("must be >= 5");
    assertThat(model).contains("must be <= 5");
  }

  @Test
  void generate_constrainedField_emitsUpperBoundValidation() {
    // INTEGER (0..255): lb=0, ub=255
    var module = new ModuleNode("M", List.of(new TypeAssignmentNode("Foo", new SequenceTypeNode(
        List.of(new SequenceFieldNode("x",
            new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new NumberBound(255))),
            false, null))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Foo").toString();
    assertThat(model).contains("> 255");
    assertThat(model).contains("must be <= 255");
  }

  @Test
  void generate_utf8StringFieldInSequence_producesJavaStringType() {
    var module = new ModuleNode("PersonInfo", List.of(new TypeAssignmentNode("Person",
        new SequenceTypeNode(List.of(
            new SequenceFieldNode("name", new Utf8StringTypeNode(Optional.empty()), false, null))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Person").toString();
    assertThat(model).contains("String name");
  }

  @Test
  void generate_utf8StringField_emitsUtf8StringHelperCalls() {
    var module = new ModuleNode("PersonInfo", List.of(new TypeAssignmentNode("Person",
        new SequenceTypeNode(List.of(
            new SequenceFieldNode("name", new Utf8StringTypeNode(Optional.empty()), false, null))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String codec = findFile(files, "PersonCodec").toString();
    assertThat(codec).contains("encodeUtf8String");
    assertThat(codec).contains("decodeUtf8String");
  }

  @Test
  void generate_utf8StringField_emitsNullValidation() {
    var module = new ModuleNode("PersonInfo", List.of(new TypeAssignmentNode("Person",
        new SequenceTypeNode(List.of(
            new SequenceFieldNode("name", new Utf8StringTypeNode(Optional.empty()), false, null))))));
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
            new SequenceFieldNode("state",
                new EnumeratedTypeNode(List.of("pending", "active", "inactive")), false, null))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Status").toString();
    assertThat(model).contains("int state");
  }

  @Test
  void generate_enumeratedFieldInSequence_emitsWriteBitsWithBitCount() {
    // 3 values: range=2, bitCount=2
    var module = new ModuleNode("StatusInfo", List.of(new TypeAssignmentNode("Status",
        new SequenceTypeNode(List.of(
            new SequenceFieldNode("state",
                new EnumeratedTypeNode(List.of("pending", "active", "inactive")), false, null))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String codec = findFile(files, "StatusCodec").toString();
    assertThat(codec).contains("writeBits(model.state(), 2)");
    assertThat(codec).contains("readBits(2)");
  }

  @Test
  void generate_enumeratedFieldInSequence_emitsLowerBoundValidation() {
    var module = new ModuleNode("StatusInfo", List.of(new TypeAssignmentNode("Status",
        new SequenceTypeNode(List.of(
            new SequenceFieldNode("state",
                new EnumeratedTypeNode(List.of("pending", "active", "inactive")), false, null))))));
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
            new SequenceFieldNode("state",
                new EnumeratedTypeNode(List.of("pending", "active", "inactive")), false, null))))));
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
            new SequenceFieldNode("state", new EnumeratedTypeNode(List.of("on", "off")), false, null))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String codec = findFile(files, "SwitchCodec").toString();
    assertThat(codec).contains("writeBits(model.state(), 1)");
  }

  @Test
  void generate_minBoundedField_producesLongTypeAndUnconstrainedEncoding() {
    // INTEGER (MIN..0): lower=MIN, upper=0 — unconstrained whole number (X.691 §12.2.3)
    var module = new ModuleNode("M", List.of(new TypeAssignmentNode("Offset", new SequenceTypeNode(
        List.of(new SequenceFieldNode("delta",
            new IntegerTypeNode(new ConstraintNode(new MinBound(), new NumberBound(0))), false, null))))));
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
        List.of(new SequenceFieldNode("delta",
            new IntegerTypeNode(new ConstraintNode(new MinBound(), new NumberBound(0))), false, null))))));
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(module);
    String model = findFile(files, "record Offset").toString();
    assertThat(model).contains("> 0");
    assertThat(model).contains("must be <= 0");
  }

  private static ModuleNode propulsionModule() {
    return new ModuleNode("VehicleModule", List.of(
        new TypeAssignmentNode("GasEngine", new SequenceTypeNode(List.of(
            new SequenceFieldNode("displacementCc",
                new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new NumberBound(8000))),
                false, null),
            new SequenceFieldNode("cylinders",
                new IntegerTypeNode(new ConstraintNode(new NumberBound(1), new NumberBound(16))),
                false, null)))),
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

  private static ModuleNode contactModule() {
    return new ModuleNode("ContactModule", List.of(new TypeAssignmentNode("Contact",
        new SequenceTypeNode(List.of(
            new SequenceFieldNode("id",
                new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new NumberBound(255))),
                false, null),
            new SequenceFieldNode("age",
                new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new NumberBound(255))),
                true, null))))));
  }

  @Test
  void generate_optionalField_producesBoxedJavaType() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(contactModule());
    String model = findFile(files, "record Contact").toString();
    assertThat(model).contains("int id");
    assertThat(model).contains("Integer age");
  }

  @Test
  void generate_optionalField_emitsPreambleBitAndGuardedEncode() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(contactModule());
    String codec = findFile(files, "ContactCodec").toString();
    assertThat(codec).contains("out.writeBits(model.age() != null ? 1 : 0, 1)");
    assertThat(codec).contains("if (model.age() != null)");
  }

  @Test
  void generate_optionalField_emitsPresenceGuardedDecode() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(contactModule());
    String codec = findFile(files, "ContactCodec").toString();
    assertThat(codec).contains("boolean agePresent = in.readBits(1) != 0");
    assertThat(codec).contains("agePresent ?");
  }

  @Test
  void generate_optionalField_skipsNullValidationWhenAbsent() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(contactModule());
    String model = findFile(files, "record Contact").toString();
    assertThat(model).contains("if (age != null)");
  }

  private static ModuleNode settingsModule() {
    return new ModuleNode("SettingsModule", List.of(new TypeAssignmentNode("Settings",
        new SequenceTypeNode(List.of(
            new SequenceFieldNode("id",
                new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new NumberBound(255))),
                false, null),
            new SequenceFieldNode("volume",
                new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new NumberBound(100))),
                false, new IntegerDefaultValueNode(50)),
            new SequenceFieldNode("muted", new BooleanTypeNode(), false,
                new BooleanDefaultValueNode(false)))))));
  }

  @Test
  void generate_defaultField_staysUnboxedJavaType() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(settingsModule());
    String model = findFile(files, "record Settings").toString();
    assertThat(model).contains("int volume");
    assertThat(model).contains("boolean muted");
  }

  @Test
  void generate_defaultField_emitsPreambleBitComparingToDefault() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(settingsModule());
    String codec = findFile(files, "SettingsCodec").toString();
    assertThat(codec).contains("out.writeBits(model.volume() != 50 ? 1 : 0, 1)");
    assertThat(codec).contains("out.writeBits(model.muted() != false ? 1 : 0, 1)");
    assertThat(codec).contains("if (model.volume() != 50)");
    assertThat(codec).contains("if (model.muted() != false)");
  }

  @Test
  void generate_defaultField_emitsPresenceGuardedDecodeWithDefaultFallback() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(settingsModule());
    String codec = findFile(files, "SettingsCodec").toString();
    assertThat(codec).contains("boolean volumePresent = in.readBits(1) != 0");
    assertThat(codec).contains("boolean mutedPresent = in.readBits(1) != 0");
    assertThat(codec).contains("volumePresent ?");
    assertThat(codec).contains(": 50");
    assertThat(codec).contains("mutedPresent ?");
    assertThat(codec).contains(": false");
  }

  private static ModuleNode profileModule() {
    return new ModuleNode("ProfileModule", List.of(new TypeAssignmentNode("Profile",
        new SequenceTypeNode(List.of(
            new SequenceFieldNode("id",
                new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new NumberBound(255))),
                false, null),
            new SequenceFieldNode("status",
                new EnumeratedTypeNode(List.of("pending", "active", "inactive")), false,
                new EnumeratedDefaultValueNode("active")),
            new SequenceFieldNode("nickname", new Utf8StringTypeNode(Optional.empty()), false,
                new StringDefaultValueNode("anonymous")))))));
  }

  @Test
  void generate_enumeratedDefaultField_staysUnboxedJavaType() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(profileModule());
    String model = findFile(files, "record Profile").toString();
    assertThat(model).contains("int status");
    assertThat(model).contains("String nickname");
  }

  @Test
  void generate_enumeratedDefaultField_emitsPreambleBitComparingToOrdinal() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(profileModule());
    String codec = findFile(files, "ProfileCodec").toString();
    assertThat(codec).contains("out.writeBits(model.status() != 1 ? 1 : 0, 1)");
    assertThat(codec).contains("if (model.status() != 1)");
  }

  @Test
  void generate_enumeratedDefaultField_emitsPresenceGuardedDecodeWithOrdinalFallback() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(profileModule());
    String codec = findFile(files, "ProfileCodec").toString();
    assertThat(codec).contains("boolean statusPresent = in.readBits(1) != 0");
    assertThat(codec).contains("statusPresent ?");
    assertThat(codec).contains(": 1");
  }

  @Test
  void generate_stringDefaultField_emitsPreambleBitComparingByEquality() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(profileModule());
    String codec = findFile(files, "ProfileCodec").toString();
    assertThat(codec).contains("out.writeBits(!model.nickname().equals(\"anonymous\") ? 1 : 0, 1)");
    assertThat(codec).contains("if (!model.nickname().equals(\"anonymous\"))");
  }

  @Test
  void generate_stringDefaultField_emitsPresenceGuardedDecodeWithStringFallback() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(profileModule());
    String codec = findFile(files, "ProfileCodec").toString();
    assertThat(codec).contains("boolean nicknamePresent = in.readBits(1) != 0");
    assertThat(codec).contains("nicknamePresent ?");
    assertThat(codec).contains(": \"anonymous\"");
  }

  private static ModuleNode nestedReferenceModule() {
    return new ModuleNode("ContainerModule", List.of(
        new TypeAssignmentNode("Inner", new SequenceTypeNode(List.of(
            new SequenceFieldNode("value",
                new IntegerTypeNode(new ConstraintNode(new NumberBound(0), new NumberBound(255))),
                false, null)))),
        new TypeAssignmentNode("Outer", new SequenceTypeNode(List.of(
            new SequenceFieldNode("inner", new TypeReferenceNode("Inner"), false, null))))));
  }

  @Test
  void generate_typeReferenceFieldInSequence_emitsNullValidation() {
    var files = new Asn1CodeGenerator(new JavaPackage("io.example")).generate(nestedReferenceModule());
    String model = findFile(files, "record Outer").toString();
    assertThat(model).contains("== null");
    assertThat(model).contains("inner must not be null");
  }
}
