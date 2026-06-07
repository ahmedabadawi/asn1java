package io.github.ahmedabadawi.asn1java.core.validation;

import io.github.ahmedabadawi.asn1java.core.ast.BooleanTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ConstraintNode;
import io.github.ahmedabadawi.asn1java.core.ast.EnumeratedTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.FieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MaxBound;
import io.github.ahmedabadawi.asn1java.core.ast.MinBound;
import io.github.ahmedabadawi.asn1java.core.ast.ModuleNode;
import io.github.ahmedabadawi.asn1java.core.ast.NumberBound;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;
import io.github.ahmedabadawi.asn1java.core.ast.Utf8StringTypeNode;
import io.github.ahmedabadawi.asn1java.core.exception.Asn1SemanticException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Asn1SemanticValidator {

  public void validate(ModuleNode module) {
    List<ValidationError> errors = new ArrayList<>();

    checkDuplicateTypeNames(module, errors);

    for (TypeAssignmentNode type : module.types()) {
      switch (type.type()) {
        case SequenceTypeNode seq -> checkSequence(type.name(), seq, errors);
        case IntegerTypeNode it -> checkConstraint(type.name(), it.constraint(), errors);
        case BooleanTypeNode ignored -> {
        }
        case Utf8StringTypeNode ignored -> {
        }
        case EnumeratedTypeNode enumType -> checkEnumerated(type.name(), enumType, errors);
      }
    }

    if (!errors.isEmpty()) {
      throw new Asn1SemanticException(errors);
    }
  }

  private void checkDuplicateTypeNames(ModuleNode module, List<ValidationError> errors) {
    Set<String> seen = new HashSet<>();
    for (TypeAssignmentNode type : module.types()) {
      if (!seen.add(type.name())) {
        errors.add(new ValidationError("Duplicate type name: " + type.name()));
      }
    }
  }

  private void checkSequence(String typeName, SequenceTypeNode seq, List<ValidationError> errors) {
    Set<String> seen = new HashSet<>();
    for (FieldNode field : seq.fields()) {
      if (!seen.add(field.name())) {
        errors.add(
            new ValidationError("Duplicate field name '" + field.name() + "' in type " + typeName));
      }
      switch (field.type()) {
        case IntegerTypeNode it ->
            checkConstraint(typeName + "." + field.name(), it.constraint(), errors);
        case SequenceTypeNode st -> checkSequence(typeName + "." + field.name(), st, errors);
        case BooleanTypeNode ignored -> {
        }
        case Utf8StringTypeNode ignored -> {
        }
        case EnumeratedTypeNode enumType ->
            checkEnumerated(typeName + "." + field.name(), enumType, errors);
      }
    }
  }

  private void checkEnumerated(String location, EnumeratedTypeNode enumType,
      List<ValidationError> errors) {
    if (enumType.values().isEmpty()) {
      errors.add(new ValidationError(
          "ENUMERATED type at %s must have at least one value".formatted(location)));
      return;
    }
    Set<String> seen = new HashSet<>();
    for (String value : enumType.values()) {
      if (!seen.add(value)) {
        errors.add(new ValidationError(
            "Duplicate enumeration value '%s' in %s".formatted(value, location)));
      }
    }
  }

  private void checkConstraint(String location, ConstraintNode constraint,
      List<ValidationError> errors) {
    if (constraint.lowerBound() instanceof NumberBound lowerBound
        && constraint.upperBound() instanceof NumberBound upperBound
        && upperBound.value() < lowerBound.value()) {
      errors.add(new ValidationError(
          "Inverted constraint bounds at %s: lower=%d > upper=%d"
              .formatted(location, lowerBound.value(), upperBound.value())));
    }
  }
}
