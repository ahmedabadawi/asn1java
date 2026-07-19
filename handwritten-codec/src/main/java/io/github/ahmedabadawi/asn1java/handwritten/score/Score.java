package io.github.ahmedabadawi.asn1java.handwritten.score;

public record Score(int level, int points, int offset) {

  public Score {
    if (level < 1) {
      throw new IllegalArgumentException("level must be >= 1");
    }
    if (level > 10) {
      throw new IllegalArgumentException("level must be <= 10");
    }
    if (points < 0) {
      throw new IllegalArgumentException("points must be >= 0");
    }
    if (points > 999) {
      throw new IllegalArgumentException("points must be <= 999");
    }
    if (offset < -10) {
      throw new IllegalArgumentException("offset must be >= -10");
    }
    if (offset > 10) {
      throw new IllegalArgumentException("offset must be <= 10");
    }
  }
}
