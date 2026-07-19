package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.Test;

class ElectricMotorTest {

  @Test
  void construct_WhenPowerKwExceedsMax_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new ElectricMotor(1001, 75));

    // Then
    assertThat(thrown).hasMessageContaining("powerKw must be <= 1000");
  }

  @Test
  void construct_WhenBatteryKwhExceedsMax_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new ElectricMotor(150, 501));

    // Then
    assertThat(thrown).hasMessageContaining("batteryKwh must be <= 500");
  }
}
