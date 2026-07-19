package io.github.ahmedabadawi.asn1java.handwritten.transport;

public record ProtocolVersion(VersionSingle major, VersionSingle minor) {

  public ProtocolVersion {
    if (major == null) {
      throw new IllegalArgumentException("major must not be null");
    }
    if (minor == null) {
      throw new IllegalArgumentException("minor must not be null");
    }
  }
}
