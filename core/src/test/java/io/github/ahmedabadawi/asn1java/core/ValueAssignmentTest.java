package io.github.ahmedabadawi.asn1java.core;

import io.github.ahmedabadawi.asn1java.core.ast.IntegerTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.NumberBound;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceOfTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class ValueAssignmentTest {

  @Test
  void parse_WhenConstantUsedInIntegerBound_ThenResolvedToLiteralNumberBound() {
    // Given
    String source = """
        LimitsModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            maxVolume INTEGER ::= 100

            Settings ::= SEQUENCE {
                volume INTEGER (0..maxVolume)
            }
        END
        """;

    // When
    var module = Asn1Spec.parse(source);

    // Then
    var settings = module.types().getFirst();
    var sequence = (SequenceTypeNode) settings.type();
    var volume = (IntegerTypeNode) sequence.fields().getFirst().type();
    assertThat(volume.constraint().lowerBound()).isEqualTo(new NumberBound(0));
    assertThat(volume.constraint().upperBound()).isEqualTo(new NumberBound(100));
  }

  @Test
  void parse_WhenConstantUsedInSizeBound_ThenResolvedToLiteralNumberBound() {
    // Given
    String source = """
        LimitsModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            maxTags INTEGER ::= 10

            Settings ::= SEQUENCE {
                tags SEQUENCE (SIZE (0..maxTags)) OF UTF8String
            }
        END
        """;

    // When
    var module = Asn1Spec.parse(source);

    // Then
    var settings = module.types().getFirst();
    var sequence = (SequenceTypeNode) settings.type();
    var tags = (SequenceOfTypeNode) sequence.fields().getFirst().type();
    assertThat(tags.sizeConstraint()).isPresent();
    assertThat(tags.sizeConstraint().get().lowerBound()).isEqualTo(new NumberBound(0));
    assertThat(tags.sizeConstraint().get().upperBound()).isEqualTo(new NumberBound(10));
  }

  @Test
  void parse_WhenConstantIsUndefined_ThenThrowsIllegalArgumentException() {
    // Given
    String source = """
        LimitsModule DEFINITIONS AUTOMATIC TAGS ::= BEGIN
            Settings ::= SEQUENCE {
                volume INTEGER (0..undefinedConst)
            }
        END
        """;

    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> Asn1Spec.parse(source));

    // Then
    assertThat(thrown).hasMessageContaining("Undefined constant 'undefinedConst'");
  }
}
