package io.github.ahmedabadawi.asn1java.core.ast;

import java.util.Optional;

public record BitStringTypeNode(Optional<ConstraintNode> sizeConstraint) implements TypeNode {
}
