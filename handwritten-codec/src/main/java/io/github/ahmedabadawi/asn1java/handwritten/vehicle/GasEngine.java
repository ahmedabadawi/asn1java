package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

public record GasEngine(int displacementCc, int cylinders) {

  private static final int DISPLACEMENT_CC_MAX = 8000;
  private static final int CYLINDERS_MAX = 16;

  public GasEngine {
    if (displacementCc < 0) {
      throw new IllegalArgumentException("displacementCc must be >= 0");
    }
    if (displacementCc > DISPLACEMENT_CC_MAX) {
      throw new IllegalArgumentException(
          "displacementCc must be <= %d".formatted(DISPLACEMENT_CC_MAX));
    }
    if (cylinders < 1) {
      throw new IllegalArgumentException("cylinders must be >= 1");
    }
    if (cylinders > CYLINDERS_MAX) {
      throw new IllegalArgumentException("cylinders must be <= %d".formatted(CYLINDERS_MAX));
    }
  }
}
