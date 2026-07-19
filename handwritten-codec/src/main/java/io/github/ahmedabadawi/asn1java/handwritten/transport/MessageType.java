package io.github.ahmedabadawi.asn1java.handwritten.transport;

import java.nio.charset.StandardCharsets;

public record MessageType(String value) {

  public MessageType {
    if (value == null) {
      throw new IllegalArgumentException("value must not be null");
    }
    int byteLength = value.getBytes(StandardCharsets.UTF_8).length;
    if (byteLength < 2) {
      throw new IllegalArgumentException("value length must be >= 2");
    }
    if (byteLength > 9) {
      throw new IllegalArgumentException("value length must be <= 9");
    }
  }
}
