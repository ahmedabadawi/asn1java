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
}
