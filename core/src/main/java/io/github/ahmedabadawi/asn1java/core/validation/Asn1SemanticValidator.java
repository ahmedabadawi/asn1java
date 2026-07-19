package io.github.ahmedabadawi.asn1java.core.validation;

import io.github.ahmedabadawi.asn1java.core.ast.BooleanTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ChoiceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ConstraintNode;
import io.github.ahmedabadawi.asn1java.core.ast.EnumeratedTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.FieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.BitStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.Ia5StringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.NullTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.VisibleStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MaxBound;
import io.github.ahmedabadawi.asn1java.core.ast.MinBound;
import io.github.ahmedabadawi.asn1java.core.ast.ModuleNode;
import io.github.ahmedabadawi.asn1java.core.ast.NumberBound;
import io.github.ahmedabadawi.asn1java.core.ast.OctetStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceFieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeReferenceNode;
import io.github.ahmedabadawi.asn1java.core.ast.Utf8StringTypeNode;
import io.github.ahmedabadawi.asn1java.core.exception.Asn1SemanticException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Asn1SemanticValidator {

  public void validate(ModuleNode module) {
    List<ValidationError> errors = new ArrayList<>();

    checkDuplicateTypeNames(module, errors);

    Set<String> definedTypeNames = module.types().stream()
        .map(TypeAssignmentNode::name)
        .collect(Collectors.toSet());

    for (TypeAssignmentNode type : module.types()) {
      switch (type.type()) {
        case SequenceTypeNode seq -> checkSequence(type.name(), seq, definedTypeNames, errors);
        case ChoiceTypeNode choice -> checkChoice(type.name(), choice, definedTypeNames, errors);
        case IntegerTypeNode it -> checkConstraint(type.name(), it.constraint(), errors);
        case BooleanTypeNode ignored -> {
        }
        case Utf8StringTypeNode utf8Type ->
            utf8Type.sizeConstraint().ifPresent(c -> checkConstraint(type.name(), c, errors));
        case OctetStringTypeNode octetType ->
            octetType.sizeConstraint().ifPresent(c -> checkConstraint(type.name(), c, errors));
        case BitStringTypeNode bitType ->
            bitType.sizeConstraint().ifPresent(c -> checkConstraint(type.name(), c, errors));
        case NullTypeNode ignored -> {
        }
        case Ia5StringTypeNode ia5Type ->
            ia5Type.sizeConstraint().ifPresent(c -> checkConstraint(type.name(), c, errors));
        case VisibleStringTypeNode visibleType ->
            visibleType.sizeConstraint().ifPresent(c -> checkConstraint(type.name(), c, errors));
        case EnumeratedTypeNode enumType -> checkEnumerated(type.name(), enumType, errors);
        case TypeReferenceNode ignored -> {
        }
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

  private void checkSequence(String typeName, SequenceTypeNode seq,
      Set<String> definedTypeNames, List<ValidationError> errors) {
    Set<String> seen = new HashSet<>();
    for (SequenceFieldNode field : seq.fields()) {
      if (!seen.add(field.name())) {
        errors.add(
            new ValidationError("Duplicate field name '" + field.name() + "' in type " + typeName));
      }
      if (field.optional() && field.type() instanceof NullTypeNode) {
        errors.add(new ValidationError(
            "Field '%s' in type %s cannot be OPTIONAL on a NULL type"
                .formatted(field.name(), typeName)));
      }
      checkFieldType(typeName + "." + field.name(), field.type(), definedTypeNames, errors);
    }
  }

  private void checkChoice(String typeName, ChoiceTypeNode choice,
      Set<String> definedTypeNames, List<ValidationError> errors) {
    if (choice.alternatives().isEmpty()) {
      errors.add(new ValidationError(
          "CHOICE type at %s must have at least one alternative".formatted(typeName)));
      return;
    }
    Set<String> seen = new HashSet<>();
    for (FieldNode alternative : choice.alternatives()) {
      if (!seen.add(alternative.name())) {
        errors.add(new ValidationError(
            "Duplicate alternative name '" + alternative.name() + "' in type " + typeName));
      }
      checkFieldType(typeName + "." + alternative.name(), alternative.type(), definedTypeNames,
          errors);
    }
  }

  private void checkFieldType(String location, TypeNode type, Set<String> definedTypeNames,
      List<ValidationError> errors) {
    switch (type) {
      case IntegerTypeNode it -> checkConstraint(location, it.constraint(), errors);
      case SequenceTypeNode st -> checkSequence(location, st, definedTypeNames, errors);
      case ChoiceTypeNode choice -> checkChoice(location, choice, definedTypeNames, errors);
      case BooleanTypeNode ignored -> {
      }
      case Utf8StringTypeNode utf8Type ->
          utf8Type.sizeConstraint().ifPresent(c -> checkConstraint(location, c, errors));
      case OctetStringTypeNode octetType ->
          octetType.sizeConstraint().ifPresent(c -> checkConstraint(location, c, errors));
      case BitStringTypeNode bitType ->
          bitType.sizeConstraint().ifPresent(c -> checkConstraint(location, c, errors));
      case NullTypeNode ignored -> {
      }
      case Ia5StringTypeNode ia5Type ->
          ia5Type.sizeConstraint().ifPresent(c -> checkConstraint(location, c, errors));
      case VisibleStringTypeNode visibleType ->
          visibleType.sizeConstraint().ifPresent(c -> checkConstraint(location, c, errors));
      case EnumeratedTypeNode enumType -> checkEnumerated(location, enumType, errors);
      case TypeReferenceNode ref -> {
        if (!definedTypeNames.contains(ref.typeName())) {
          errors.add(new ValidationError(
              "Unknown type reference '%s' in field %s".formatted(ref.typeName(), location)));
        }
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
