package io.github.ahmedabadawi.asn1java.handwritten.priority;

public record Priority(int level, long adjustment) {

  private static final int LEVEL_MAX = 2;
  private static final long ADJUSTMENT_MAX = 0;

  public Priority {
    if (level < 0 || level > LEVEL_MAX) {
      throw new IllegalArgumentException(
          "level must be in range 0..%d".formatted(LEVEL_MAX));
    }
    if (adjustment > ADJUSTMENT_MAX) {
      throw new IllegalArgumentException(
          "adjustment must be <= %d".formatted(ADJUSTMENT_MAX));
    }
  }
}
