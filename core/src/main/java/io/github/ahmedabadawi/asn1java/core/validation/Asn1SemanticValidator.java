package io.github.ahmedabadawi.asn1java.core.validation;

import io.github.ahmedabadawi.asn1java.core.ast.*;
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
                case IntegerTypeNode it   -> checkConstraint(type.name(), it.constraint(), errors);
                case BooleanTypeNode ignored -> {}
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
                errors.add(new ValidationError(
                        "Duplicate field name '" + field.name() + "' in type " + typeName));
            }
            switch (field.type()) {
                case IntegerTypeNode it   -> checkConstraint(typeName + "." + field.name(), it.constraint(), errors);
                case SequenceTypeNode st  -> checkSequence(typeName + "." + field.name(), st, errors);
                case BooleanTypeNode ignored -> {}
            }
        }
    }

    private void checkConstraint(String location, ConstraintNode constraint, List<ValidationError> errors) {
        switch (constraint.upperBound()) {
            case NumberBound nb when nb.value() < constraint.lowerBound() ->
                errors.add(new ValidationError(
                        "Inverted constraint bounds at " + location +
                        ": lower=" + constraint.lowerBound() + " > upper=" + nb.value()));
            case NumberBound nb -> {}
            case MaxBound mb    -> {}
        }
    }
}
