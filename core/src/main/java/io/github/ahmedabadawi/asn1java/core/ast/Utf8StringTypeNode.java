package io.github.ahmedabadawi.asn1java.core.ast;

import java.util.Optional;

public record Utf8StringTypeNode(Optional<ConstraintNode> sizeConstraint) implements TypeNode {
}
