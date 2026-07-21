package io.github.ahmedabadawi.asn1java.handwritten.limits;

import java.util.List;

public record Settings(int volume, List<String> tags) {

  public Settings {
    if (volume < 0) {
      throw new IllegalArgumentException("volume must be >= 0");
    }
    if (volume > 100) {
      throw new IllegalArgumentException("volume must be <= 100");
    }
    if (tags == null) {
      throw new IllegalArgumentException("tags must not be null");
    }
    if (tags.size() > 10) {
      throw new IllegalArgumentException("tags must have between 0 and 10 elements");
    }
  }
}
