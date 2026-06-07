package io.github.ahmedabadawi.asn1java.core.ast;

import java.util.Optional;

public record VisibleStringTypeNode(Optional<ConstraintNode> sizeConstraint) implements TypeNode {
}
