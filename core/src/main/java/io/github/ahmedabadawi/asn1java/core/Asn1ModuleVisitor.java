package io.github.ahmedabadawi.asn1java.core;

import io.github.ahmedabadawi.asn1java.core.ast.*;

import java.util.List;

public class Asn1ModuleVisitor extends ASN1BaseVisitor<Object> {

    @Override
    public ModuleNode visitModuleDefinition(ASN1Parser.ModuleDefinitionContext ctx) {
        String name = ctx.moduleIdentifier().UPPER_IDENT().getText();
        List<TypeAssignmentNode> types = ctx.memberList().typeAssignment().stream()
                .map(t -> switch (visit(t)) {
                    case TypeAssignmentNode n -> n;
                    default -> throw new IllegalStateException("unexpected node for typeAssignment: " + t.getText());
                })
                .toList();
        return new ModuleNode(name, types);
    }

    @Override
    public TypeAssignmentNode visitTypeAssignment(ASN1Parser.TypeAssignmentContext ctx) {
        String name = ctx.UPPER_IDENT().getText();
        TypeNode type = switch (visit(ctx.sequenceType())) {
            case TypeNode t -> t;
            default -> throw new IllegalStateException("unexpected node for sequenceType: " + ctx.sequenceType().getText());
        };
        return new TypeAssignmentNode(name, type);
    }

    @Override
    public SequenceTypeNode visitSequenceType(ASN1Parser.SequenceTypeContext ctx) {
        List<FieldNode> fields = ctx.fieldList().field().stream()
                .map(f -> switch (visit(f)) {
                    case FieldNode n -> n;
                    default -> throw new IllegalStateException("unexpected node for field: " + f.getText());
                })
                .toList();
        return new SequenceTypeNode(fields);
    }

    @Override
    public FieldNode visitField(ASN1Parser.FieldContext ctx) {
        String name = ctx.LOWER_IDENT().getText();
        TypeNode type = switch (visit(ctx.integerType())) {
            case TypeNode t -> t;
            default -> throw new IllegalStateException("unexpected node for integerType: " + ctx.integerType().getText());
        };
        return new FieldNode(name, type);
    }

    @Override
    public IntegerTypeNode visitIntegerType(ASN1Parser.IntegerTypeContext ctx) {
        ConstraintNode constraint = switch (visit(ctx.constraint())) {
            case ConstraintNode c -> c;
            default -> throw new IllegalStateException("unexpected node for constraint: " + ctx.constraint().getText());
        };
        return new IntegerTypeNode(constraint);
    }

    @Override
    public ConstraintNode visitConstraint(ASN1Parser.ConstraintContext ctx) {
        int lower = Integer.parseInt(ctx.lowerBound().NUMBER().getText());
        Bound upper = switch (visit(ctx.upperBound())) {
            case Bound b -> b;
            default -> throw new IllegalStateException("unexpected node for upperBound: " + ctx.upperBound().getText());
        };
        return new ConstraintNode(lower, upper);
    }

    @Override
    public Bound visitUpperBound(ASN1Parser.UpperBoundContext ctx) {
        return ctx.MAX() != null
                ? new MaxBound()
                : new NumberBound(Integer.parseInt(ctx.NUMBER().getText()));
    }
}
