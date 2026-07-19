package io.github.ahmedabadawi.asn1java.handwritten.transport;

public record VersionSingle(int value) {

  public VersionSingle {
    if (value < 0) {
      throw new IllegalArgumentException("value must be >= 0");
    }
    if (value > 255) {
      throw new IllegalArgumentException("value must be <= 255");
    }
  }
}
