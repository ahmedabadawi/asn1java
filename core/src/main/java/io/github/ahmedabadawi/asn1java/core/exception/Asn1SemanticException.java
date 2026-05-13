package io.github.ahmedabadawi.asn1java.core.exception;

import io.github.ahmedabadawi.asn1java.core.validation.ValidationError;

import java.util.List;

public class Asn1SemanticException extends IllegalArgumentException {

  private final List<ValidationError> errors;

  public Asn1SemanticException(List<ValidationError> errors) {
    super("Semantic validation failed:\n" + errors.stream().map(ValidationError::message)
        .reduce((a, b) -> a + "\n" + b).orElse(""));
    this.errors = List.copyOf(errors);
  }

  public List<ValidationError> errors() {
    return errors;
  }
}
