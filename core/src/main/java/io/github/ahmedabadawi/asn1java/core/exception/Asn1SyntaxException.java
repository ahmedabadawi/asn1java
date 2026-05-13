package io.github.ahmedabadawi.asn1java.core.exception;

public class Asn1SyntaxException extends IllegalArgumentException {

  private final int line;
  private final int charPosition;

  public Asn1SyntaxException(int line, int charPosition, String message) {
    super("Syntax error at line " + line + ":" + charPosition + " — " + message);
    this.line = line;
    this.charPosition = charPosition;
  }

  public int line() {
    return line;
  }

  public int charPosition() {
    return charPosition;
  }
}
