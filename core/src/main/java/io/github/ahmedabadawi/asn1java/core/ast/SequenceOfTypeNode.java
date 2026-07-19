package io.github.ahmedabadawi.asn1java.core.ast;

import java.util.Optional;

public record SequenceOfTypeNode(TypeNode elementType, Optional<ConstraintNode> sizeConstraint)
    implements TypeNode {
}
