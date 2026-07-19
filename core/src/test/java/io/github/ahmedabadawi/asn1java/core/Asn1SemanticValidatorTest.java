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
  void validate_WhenDuplicateAlternativeName_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Selection ::= CHOICE {
                option BOOLEAN,
                option INTEGER (0..MAX)
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message)
        .containsExactly("Duplicate alternative name 'option' in type Selection");
  }

  @Test
  void validate_WhenChoiceAlternativeReferencesUnknownType_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Selection ::= CHOICE {
                option Missing
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message)
        .containsExactly("Unknown type reference 'Missing' in field Selection.option");
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

  @Test
  void validate_WhenOptionalAppliedToNullField_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Marker ::= SEQUENCE {
                tag NULL OPTIONAL
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message)
        .containsExactly("Field 'tag' in type Marker cannot be OPTIONAL or DEFAULT on a NULL type");
  }

  @Test
  void validate_WhenIntegerDefaultOnBooleanField_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Settings ::= SEQUENCE {
                muted BOOLEAN DEFAULT 1
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message)
        .containsExactly(
            "DEFAULT value at Settings.muted is an integer literal but the field is not INTEGER");
  }

  @Test
  void validate_WhenBooleanDefaultOnIntegerField_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Settings ::= SEQUENCE {
                volume INTEGER (0..100) DEFAULT FALSE
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message)
        .containsExactly(
            "DEFAULT value at Settings.volume is a boolean literal but the field is not BOOLEAN");
  }

  @Test
  void validate_WhenIntegerDefaultExceedsUpperBound_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Settings ::= SEQUENCE {
                volume INTEGER (0..100) DEFAULT 150
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message)
        .containsExactly("DEFAULT value 150 at Settings.volume exceeds the field's upper bound 100");
  }

  @Test
  void validate_WhenIntegerDefaultBelowLowerBound_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Settings ::= SEQUENCE {
                volume INTEGER (10..100) DEFAULT 5
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message)
        .containsExactly("DEFAULT value 5 at Settings.volume is below the field's lower bound 10");
  }

  @Test
  void validate_WhenEnumeratedDefaultOnNonEnumeratedField_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Profile ::= SEQUENCE {
                status INTEGER (0..2) DEFAULT active
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message)
        .containsExactly(
            "DEFAULT value at Profile.status is an enumeration identifier but the field is not ENUMERATED");
  }

  @Test
  void validate_WhenEnumeratedDefaultIsNotADeclaredValue_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Profile ::= SEQUENCE {
                status ENUMERATED { pending, active, inactive } DEFAULT unknown
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message)
        .containsExactly(
            "DEFAULT value 'unknown' at Profile.status is not a declared value of the field's ENUMERATED type");
  }

  @Test
  void validate_WhenStringDefaultOnNonStringField_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Profile ::= SEQUENCE {
                id INTEGER (0..255) DEFAULT "1"
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message)
        .containsExactly(
            "DEFAULT value at Profile.id is a string literal but the field is not a string type");
  }

  @Test
  void validate_WhenSequenceOfSizeConstraintInverted_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Playlist ::= SEQUENCE {
                tags SEQUENCE (SIZE (5..1)) OF INTEGER (0..MAX)
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message).first()
        .asString().contains("Inverted constraint bounds at Playlist.tags");
  }

  @Test
  void validate_WhenSequenceOfElementReferencesUnknownType_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Playlist ::= SEQUENCE {
                tracks SEQUENCE OF Missing
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message)
        .containsExactly("Unknown type reference 'Missing' in field Playlist.tracks[]");
  }

  @Test
  void validate_WhenTypeReferenceIsImported_ThenNoExceptionThrown() {
    // Given
    String source = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
        IMPORTS Foo FROM OtherModule;
            Bar ::= SEQUENCE {
                value Foo
            }
        END
        """;

    // When / Then
    assertThatNoException().isThrownBy(() -> Asn1Spec.parse(source));
  }

  @Test
  void validate_WhenImportedTypeCollidesWithLocalType_ThenThrowsAsn1SemanticException() {
    // Given
    String invalid = """
        MyModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
        IMPORTS Foo FROM OtherModule;
            Foo ::= SEQUENCE {
                value INTEGER (0..MAX)
            }
        END
        """;

    // When
    var exception =
        catchThrowableOfType(Asn1SemanticException.class, () -> Asn1Spec.parse(invalid));

    // Then
    assertThat(exception.errors()).hasSize(1).extracting(ValidationError::message)
        .containsExactly(
            "Imported type name 'Foo' collides with a locally-defined or previously-imported type");
  }
}
