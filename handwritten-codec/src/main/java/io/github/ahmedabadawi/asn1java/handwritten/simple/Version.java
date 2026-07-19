package io.github.ahmedabadawi.asn1java.handwritten.simple;

public record Version(int major, int minor) {

  public Version {
    if (major < 0 || minor < 0) {
      throw new IllegalArgumentException("major and minor must be >= 0");
    }
  }
}
