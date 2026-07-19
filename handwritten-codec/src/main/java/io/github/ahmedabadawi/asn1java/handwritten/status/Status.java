package io.github.ahmedabadawi.asn1java.handwritten.status;

public record Status(int state) {

  private static final int STATE_MAX = 2;

  public Status {
    if (state < 0 || state > STATE_MAX) {
      throw new IllegalArgumentException(
          "state must be in range 0..%d".formatted(STATE_MAX));
    }
  }
}
