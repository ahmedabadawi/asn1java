package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.Test;

class GasEngineTest {

  @Test
  void construct_WhenCylindersIsZero_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new GasEngine(1600, 0));

    // Then
    assertThat(thrown).hasMessageContaining("cylinders must be >= 1");
  }

  @Test
  void construct_WhenCylindersExceedsMax_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new GasEngine(1600, 17));

    // Then
    assertThat(thrown).hasMessageContaining("cylinders must be <= 16");
  }

  @Test
  void construct_WhenDisplacementCcExceedsMax_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new GasEngine(8001, 4));

    // Then
    assertThat(thrown).hasMessageContaining("displacementCc must be <= 8000");
  }
}
