package io.github.ahmedabadawi.asn1java.core.ast;

import java.util.Optional;

public record Ia5StringTypeNode(Optional<ConstraintNode> sizeConstraint) implements TypeNode {
}
