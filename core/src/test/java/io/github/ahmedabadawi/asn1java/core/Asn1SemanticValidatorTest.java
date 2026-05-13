package io.github.ahmedabadawi.asn1java.core;

import io.github.ahmedabadawi.asn1java.core.exception.Asn1SemanticException;
import io.github.ahmedabadawi.asn1java.core.validation.ValidationError;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class Asn1SemanticValidatorTest {

  private static String simpleAsn() throws IOException {
    return Files.readString(
        Paths.get(System.getProperty("user.dir")).getParent().resolve("spec/simple.asn"));
  }

  @Test
  void validate_WhenValidSpec_ThenNoExceptionThrown() throws IOException {
    // Given
    String source = simpleAsn();

    // When / Then
    assertThatNoException().isThrownBy(() -> Asn1Spec.parse(source));
  }

  @Test
  void validate_WhenDuplicateTypeName_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Version ::= SEQUENCE { major INTEGER (0..MAX) }
            Version ::= SEQUENCE { minor INTEGER (0..MAX) }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message)
        .containsExactly("Duplicate type name: Version");
  }

  @Test
  void validate_WhenDuplicateFieldName_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Version ::= SEQUENCE {
                major INTEGER (0..MAX),
                major INTEGER (0..MAX)
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message)
        .containsExactly("Duplicate field name 'major' in type Version");
  }

  @Test
  void validate_WhenConstraintBoundsInverted_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Version ::= SEQUENCE {
                major INTEGER (5..0)
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message).first()
        .asString().contains("Inverted constraint bounds at Version.major");
  }
}
