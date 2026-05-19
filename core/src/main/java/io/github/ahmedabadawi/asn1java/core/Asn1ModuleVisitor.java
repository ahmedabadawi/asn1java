package io.github.ahmedabadawi.asn1java.core;

import io.github.ahmedabadawi.asn1java.core.ast.BooleanTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.Bound;
import io.github.ahmedabadawi.asn1java.core.ast.ConstraintNode;
import io.github.ahmedabadawi.asn1java.core.ast.FieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MaxBound;
import io.github.ahmedabadawi.asn1java.core.ast.ModuleNode;
import io.github.ahmedabadawi.asn1java.core.ast.NumberBound;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.Utf8StringTypeNode;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;

public class Asn1ModuleVisitor extends ASN1BaseVisitor<Object> {

  @Override
  public ModuleNode visitModuleDefinition(ASN1Parser.ModuleDefinitionContext context) {
    String name = context.moduleIdentifier().UPPER_IDENT().getText();
    List<TypeAssignmentNode> types =
        context.memberList().typeAssignment().stream().map(t -> switch (visit(t)) {
          case TypeAssignmentNode n -> n;
          default ->
              throw new IllegalStateException("unexpected node for typeAssignment: " + t.getText());
        }).toList();
    return new ModuleNode(name, types);
  }

  @Override
  public TypeAssignmentNode visitTypeAssignment(ASN1Parser.TypeAssignmentContext context) {
    String name = context.UPPER_IDENT().getText();
    TypeNode type = switch (visit(context.sequenceType())) {
      case TypeNode t -> t;
      default -> throw new IllegalStateException(
          "unexpected node for sequenceType: " + context.sequenceType().getText());
    };
    return new TypeAssignmentNode(name, type);
  }

  @Override
  public SequenceTypeNode visitSequenceType(ASN1Parser.SequenceTypeContext context) {
    List<FieldNode> fields = context.fieldList().field().stream().map(f -> switch (visit(f)) {
      case FieldNode n -> n;
      default -> throw new IllegalStateException("unexpected node for field: " + f.getText());
    }).toList();
    return new SequenceTypeNode(fields);
  }

  @Override
  public FieldNode visitField(ASN1Parser.FieldContext context) {
    String name = context.LOWER_IDENT().getText();
    ParserRuleContext typeContext = context.integerType() != null ?
        context.integerType() :
        context.booleanType() != null ? context.booleanType() : context.utf8StringType();
    TypeNode type = switch (visit(typeContext)) {
      case TypeNode t -> t;
      default ->
          throw new IllegalStateException("unexpected node for field type: " + typeContext.getText());
    };
    return new FieldNode(name, type);
  }

  @Override
  public BooleanTypeNode visitBooleanType(ASN1Parser.BooleanTypeContext context) {
    return new BooleanTypeNode();
  }

  @Override
  public Utf8StringTypeNode visitUtf8StringType(ASN1Parser.Utf8StringTypeContext context) {
    return new Utf8StringTypeNode();
  }

  @Override
  public IntegerTypeNode visitIntegerType(ASN1Parser.IntegerTypeContext context) {
    ConstraintNode constraint = switch (visit(context.constraint())) {
      case ConstraintNode c -> c;
      default -> throw new IllegalStateException(
          "unexpected node for constraint: " + context.constraint().getText());
    };
    return new IntegerTypeNode(constraint);
  }

  @Override
  public ConstraintNode visitConstraint(ASN1Parser.ConstraintContext context) {
    var lowerBoundContext = context.lowerBound();
    int lowerSign = lowerBoundContext.MINUS() != null ? -1 : 1;
    int lowerMagnitude = Integer.parseInt(lowerBoundContext.NUMBER().getText());
    int lower = lowerSign * lowerMagnitude;
    Bound upper = switch (visit(context.upperBound())) {
      case Bound b -> b;
      default -> throw new IllegalStateException(
          "unexpected node for upperBound: " + context.upperBound().getText());
    };
    return new ConstraintNode(lower, upper);
  }

  @Override
  public Bound visitUpperBound(ASN1Parser.UpperBoundContext context) {
    return context.MAX() != null ?
        new MaxBound() :
        new NumberBound(Integer.parseInt(context.NUMBER().getText()));
  }
}
