package io.github.ahmedabadawi.asn1java.core;

import io.github.ahmedabadawi.asn1java.core.ast.BooleanDefaultValueNode;
import io.github.ahmedabadawi.asn1java.core.ast.BooleanTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.Bound;
import io.github.ahmedabadawi.asn1java.core.ast.ChoiceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.DefaultValueNode;
import io.github.ahmedabadawi.asn1java.core.ast.ConstraintNode;
import io.github.ahmedabadawi.asn1java.core.ast.EnumeratedTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.FieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerDefaultValueNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.BitStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.Ia5StringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.VisibleStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MaxBound;
import io.github.ahmedabadawi.asn1java.core.ast.NullTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MinBound;
import io.github.ahmedabadawi.asn1java.core.ast.OctetStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ModuleNode;
import io.github.ahmedabadawi.asn1java.core.ast.NumberBound;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceFieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeReferenceNode;
import io.github.ahmedabadawi.asn1java.core.ast.Utf8StringTypeNode;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    ParserRuleContext typeContext;
    if (context.sequenceType() != null) {
      typeContext = context.sequenceType();
    } else if (context.choiceType() != null) {
      typeContext = context.choiceType();
    } else if (context.enumeratedType() != null) {
      typeContext = context.enumeratedType();
    } else if (context.integerType() != null) {
      typeContext = context.integerType();
    } else if (context.utf8StringType() != null) {
      typeContext = context.utf8StringType();
    } else if (context.octetStringType() != null) {
      typeContext = context.octetStringType();
    } else if (context.bitStringType() != null) {
      typeContext = context.bitStringType();
    } else if (context.ia5StringType() != null) {
      typeContext = context.ia5StringType();
    } else if (context.visibleStringType() != null) {
      typeContext = context.visibleStringType();
    } else if (context.nullType() != null) {
      typeContext = context.nullType();
    } else {
      typeContext = context.booleanType();
    }
    TypeNode type = switch (visit(typeContext)) {
      case TypeNode t -> t;
      default -> throw new IllegalStateException(
          "unexpected node for type: " + typeContext.getText());
    };
    return new TypeAssignmentNode(name, type);
  }

  @Override
  public SequenceTypeNode visitSequenceType(ASN1Parser.SequenceTypeContext context) {
    List<SequenceFieldNode> fields = context.sequenceFieldList().sequenceField().stream()
        .map(f -> switch (visit(f)) {
          case SequenceFieldNode n -> n;
          default -> throw new IllegalStateException("unexpected node for field: " + f.getText());
        }).toList();
    return new SequenceTypeNode(fields);
  }

  @Override
  public ChoiceTypeNode visitChoiceType(ASN1Parser.ChoiceTypeContext context) {
    List<FieldNode> alternatives =
        context.fieldList().field().stream().map(f -> switch (visit(f)) {
          case FieldNode n -> n;
          default -> throw new IllegalStateException("unexpected node for field: " + f.getText());
        }).toList();
    return new ChoiceTypeNode(alternatives);
  }

  @Override
  public FieldNode visitField(ASN1Parser.FieldContext context) {
    String name = context.LOWER_IDENT().getText();
    TypeNode type = resolveFieldType(context.fieldType());
    return new FieldNode(name, type);
  }

  @Override
  public SequenceFieldNode visitSequenceField(ASN1Parser.SequenceFieldContext context) {
    String name = context.LOWER_IDENT().getText();
    TypeNode type = resolveFieldType(context.fieldType());
    boolean optional = context.OPTIONAL() != null;
    DefaultValueNode defaultValue =
        context.defaultValue() != null ? parseDefaultValue(context.defaultValue()) : null;
    return new SequenceFieldNode(name, type, optional, defaultValue);
  }

  private DefaultValueNode parseDefaultValue(ASN1Parser.DefaultValueContext context) {
    if (context.TRUE() != null) {
      return new BooleanDefaultValueNode(true);
    }
    if (context.FALSE() != null) {
      return new BooleanDefaultValueNode(false);
    }
    int sign = context.MINUS() != null ? -1 : 1;
    long magnitude = Long.parseLong(context.NUMBER().getText());
    return new IntegerDefaultValueNode(sign * magnitude);
  }

  private TypeNode resolveFieldType(ASN1Parser.FieldTypeContext context) {
    ParserRuleContext typeContext;
    if (context.integerType() != null) {
      typeContext = context.integerType();
    } else if (context.booleanType() != null) {
      typeContext = context.booleanType();
    } else if (context.utf8StringType() != null) {
      typeContext = context.utf8StringType();
    } else if (context.octetStringType() != null) {
      typeContext = context.octetStringType();
    } else if (context.bitStringType() != null) {
      typeContext = context.bitStringType();
    } else if (context.nullType() != null) {
      typeContext = context.nullType();
    } else if (context.ia5StringType() != null) {
      typeContext = context.ia5StringType();
    } else if (context.visibleStringType() != null) {
      typeContext = context.visibleStringType();
    } else if (context.enumeratedType() != null) {
      typeContext = context.enumeratedType();
    } else {
      return new TypeReferenceNode(context.typeReference().UPPER_IDENT().getText());
    }
    return switch (visit(typeContext)) {
      case TypeNode t -> t;
      default ->
          throw new IllegalStateException("unexpected node for field type: " + typeContext.getText());
    };
  }

  @Override
  public BooleanTypeNode visitBooleanType(ASN1Parser.BooleanTypeContext context) {
    return new BooleanTypeNode();
  }

  @Override
  public EnumeratedTypeNode visitEnumeratedType(ASN1Parser.EnumeratedTypeContext context) {
    List<String> values = context.enumValueList().enumValue().stream()
        .map(enumValue -> enumValue.LOWER_IDENT().getText())
        .collect(Collectors.toList());
    return new EnumeratedTypeNode(values);
  }

  @Override
  public Utf8StringTypeNode visitUtf8StringType(ASN1Parser.Utf8StringTypeContext context) {
    Optional<ConstraintNode> sizeConstraint = context.sizeConstraint() != null
        ? Optional.of(parseSizeConstraint(context.sizeConstraint()))
        : Optional.empty();
    return new Utf8StringTypeNode(sizeConstraint);
  }

  @Override
  public OctetStringTypeNode visitOctetStringType(ASN1Parser.OctetStringTypeContext context) {
    Optional<ConstraintNode> sizeConstraint = context.sizeConstraint() != null
        ? Optional.of(parseSizeConstraint(context.sizeConstraint()))
        : Optional.empty();
    return new OctetStringTypeNode(sizeConstraint);
  }

  @Override
  public NullTypeNode visitNullType(ASN1Parser.NullTypeContext context) {
    return new NullTypeNode();
  }

  @Override
  public Ia5StringTypeNode visitIa5StringType(ASN1Parser.Ia5StringTypeContext context) {
    Optional<ConstraintNode> sizeConstraint = context.sizeConstraint() != null
        ? Optional.of(parseSizeConstraint(context.sizeConstraint()))
        : Optional.empty();
    return new Ia5StringTypeNode(sizeConstraint);
  }

  @Override
  public VisibleStringTypeNode visitVisibleStringType(ASN1Parser.VisibleStringTypeContext context) {
    Optional<ConstraintNode> sizeConstraint = context.sizeConstraint() != null
        ? Optional.of(parseSizeConstraint(context.sizeConstraint()))
        : Optional.empty();
    return new VisibleStringTypeNode(sizeConstraint);
  }

  @Override
  public BitStringTypeNode visitBitStringType(ASN1Parser.BitStringTypeContext context) {
    Optional<ConstraintNode> sizeConstraint = context.sizeConstraint() != null
        ? Optional.of(parseSizeConstraint(context.sizeConstraint()))
        : Optional.empty();
    return new BitStringTypeNode(sizeConstraint);
  }

  private ConstraintNode parseSizeConstraint(ASN1Parser.SizeConstraintContext context) {
    Bound lower = parseLowerBound(context.lowerBound());
    Bound upper = switch (visit(context.upperBound())) {
      case Bound b -> b;
      default -> throw new IllegalStateException(
          "unexpected node for size upper bound: " + context.upperBound().getText());
    };
    return new ConstraintNode(lower, upper);
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
    Bound lower = parseLowerBound(context.lowerBound());
    Bound upper = switch (visit(context.upperBound())) {
      case Bound b -> b;
      default -> throw new IllegalStateException(
          "unexpected node for upperBound: " + context.upperBound().getText());
    };
    return new ConstraintNode(lower, upper);
  }

  private Bound parseLowerBound(ASN1Parser.LowerBoundContext context) {
    if (context.MIN() != null) {
      return new MinBound();
    }
    int sign = context.MINUS() != null ? -1 : 1;
    int magnitude = Integer.parseInt(context.NUMBER().getText());
    return new NumberBound(sign * magnitude);
  }

  @Override
  public Bound visitUpperBound(ASN1Parser.UpperBoundContext context) {
    return context.MAX() != null ?
        new MaxBound() :
        new NumberBound(Integer.parseInt(context.NUMBER().getText()));
  }
}
