package io.github.ahmedabadawi.asn1java.handwritten.flags;

public record Flags(byte[] bits) {

  private static final int BITS_SIZE = 8;

  public Flags {
    if (bits == null) {
      throw new IllegalArgumentException("bits must not be null");
    }
    if (bits.length * 8 != BITS_SIZE) {
      throw new IllegalArgumentException(
          "bits must be exactly %d bits (%d bytes)".formatted(BITS_SIZE, BITS_SIZE / 8));
    }
  }
}
