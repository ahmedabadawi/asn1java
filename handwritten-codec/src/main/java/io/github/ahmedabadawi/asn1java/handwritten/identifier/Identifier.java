package io.github.ahmedabadawi.asn1java.handwritten.identifier;

public record Identifier(String code, String label) {

  private static final int CODE_SIZE_LB = 1;
  private static final int CODE_SIZE_UB = 8;
  private static final int LABEL_SIZE_LB = 1;
  private static final int LABEL_SIZE_UB = 8;

  public Identifier {
    if (code == null) {
      throw new IllegalArgumentException("code must not be null");
    }
    if (label == null) {
      throw new IllegalArgumentException("label must not be null");
    }
    if (code.length() < CODE_SIZE_LB || code.length() > CODE_SIZE_UB) {
      throw new IllegalArgumentException(
          "code length must be in range %d..%d".formatted(CODE_SIZE_LB, CODE_SIZE_UB));
    }
    if (label.length() < LABEL_SIZE_LB || label.length() > LABEL_SIZE_UB) {
      throw new IllegalArgumentException(
          "label length must be in range %d..%d".formatted(LABEL_SIZE_LB, LABEL_SIZE_UB));
    }
  }
}
