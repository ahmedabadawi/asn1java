package io.github.ahmedabadawi.asn1java.handwritten.label;

import java.nio.charset.StandardCharsets;

public record Label(String text) {

  private static final int TEXT_SIZE_LB = 1;
  private static final int TEXT_SIZE_UB = 64;

  public Label {
    if (text == null) {
      throw new IllegalArgumentException("text must not be null");
    }
    int byteLength = text.getBytes(StandardCharsets.UTF_8).length;
    if (byteLength < TEXT_SIZE_LB) {
      throw new IllegalArgumentException(
          "text length must be >= %d".formatted(TEXT_SIZE_LB));
    }
    if (byteLength > TEXT_SIZE_UB) {
      throw new IllegalArgumentException(
          "text length must be <= %d".formatted(TEXT_SIZE_UB));
    }
  }
}
