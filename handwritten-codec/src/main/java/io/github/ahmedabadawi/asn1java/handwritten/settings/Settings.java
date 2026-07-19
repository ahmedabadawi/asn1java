package io.github.ahmedabadawi.asn1java.handwritten.settings;

public record Settings(int id, int volume, boolean muted) {

  public Settings {
    if (id < 0) {
      throw new IllegalArgumentException("id must be >= 0");
    }
    if (id > 255) {
      throw new IllegalArgumentException("id must be <= 255");
    }
    if (volume < 0) {
      throw new IllegalArgumentException("volume must be >= 0");
    }
    if (volume > 100) {
      throw new IllegalArgumentException("volume must be <= 100");
    }
  }
}
