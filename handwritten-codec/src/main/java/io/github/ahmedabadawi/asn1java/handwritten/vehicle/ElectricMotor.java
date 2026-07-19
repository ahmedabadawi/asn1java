package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

public record ElectricMotor(int powerKw, int batteryKwh) {

  public ElectricMotor {
    if (powerKw < 0) {
      throw new IllegalArgumentException("powerKw must be >= 0");
    }
    if (batteryKwh < 0) {
      throw new IllegalArgumentException("batteryKwh must be >= 0");
    }
  }
}
