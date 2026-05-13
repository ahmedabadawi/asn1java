package io.github.ahmedabadawi.asn1java.core;

import io.github.ahmedabadawi.asn1java.core.ast.FieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MaxBound;
import io.github.ahmedabadawi.asn1java.core.ast.ModuleNode;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import io.github.ahmedabadawi.asn1java.core.exception.Asn1SyntaxException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class Asn1SpecTest {

  private static String simpleAsn() throws IOException {
    return Files.readString(
        Paths.get(System.getProperty("user.dir")).getParent().resolve("spec/simple.asn"));
  }

  @Test
  void parse_WhenValidSpec_ThenModuleStructureIsExtracted() throws IOException {
    // Given
    String source = simpleAsn();

    // When
    ModuleNode module = Asn1Spec.parse(source);

    // Then
    assertThat(module.name()).isEqualTo("VersionInfo");
    assertThat(module.types()).hasSize(1);

    var version = module.types().getFirst();
    assertThat(version.name()).isEqualTo("Version");
    assertThat(version.type()).isInstanceOf(SequenceTypeNode.class);

    var sequence = (SequenceTypeNode) version.type();
    assertThat(sequence.fields())
        .hasSize(2)
        .extracting(FieldNode::name)
        .containsExactly("major", "minor");
    assertThat(sequence.fields()).allSatisfy(f -> {
      assertThat(f.type()).isInstanceOf(IntegerTypeNode.class);
      var intType = (IntegerTypeNode) f.type();
      assertThat(intType.constraint().lowerBound()).isZero();
      assertThat(intType.constraint().upperBound()).isInstanceOf(MaxBound.class);
    });
  }

  @Test
  void parse_WhenSequenceKeywordMissing_ThenThrowsAsn1SyntaxException() {
    // Given
    String invalid = """
        BadModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Version ::= {
                major INTEGER (0..MAX)
            }
        END
        """;

    // When
    var exception = catchThrowableOfType(Asn1SyntaxException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.line()).isEqualTo(2);
    assertThat(exception.charPosition()).isEqualTo(16);
    assertThat(exception).hasMessageContaining("Syntax error");
  }
}
