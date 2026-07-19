package io.github.ahmedabadawi.asn1java.handwritten.profile;

public record Profile(int id, int status, String nickname) {

  public Profile {
    if (id < 0) {
      throw new IllegalArgumentException("id must be >= 0");
    }
    if (id > 255) {
      throw new IllegalArgumentException("id must be <= 255");
    }
    if (status < 0) {
      throw new IllegalArgumentException("status must be >= 0");
    }
    if (status > 2) {
      throw new IllegalArgumentException("status must be <= 2");
    }
    if (nickname == null) {
      throw new IllegalArgumentException("nickname must not be null");
    }
  }
}
