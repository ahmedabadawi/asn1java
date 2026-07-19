package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

public record GasEngine(int displacementCc, int cylinders) {

  public GasEngine {
    if (displacementCc < 0) {
      throw new IllegalArgumentException("displacementCc must be >= 0");
    }
    if (cylinders < 1) {
      throw new IllegalArgumentException("cylinders must be >= 1");
    }
  }
}
