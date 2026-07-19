package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

public record ElectricMotor(int powerKw, int batteryKwh) {

  private static final int POWER_KW_MAX = 1000;
  private static final int BATTERY_KWH_MAX = 500;

  public ElectricMotor {
    if (powerKw < 0) {
      throw new IllegalArgumentException("powerKw must be >= 0");
    }
    if (powerKw > POWER_KW_MAX) {
      throw new IllegalArgumentException("powerKw must be <= %d".formatted(POWER_KW_MAX));
    }
    if (batteryKwh < 0) {
      throw new IllegalArgumentException("batteryKwh must be >= 0");
    }
    if (batteryKwh > BATTERY_KWH_MAX) {
      throw new IllegalArgumentException("batteryKwh must be <= %d".formatted(BATTERY_KWH_MAX));
    }
  }
}
