package io.github.ahmedabadawi.asn1java.core.ast;

import java.util.List;

public record ChoiceTypeNode(List<FieldNode> alternatives) implements TypeNode {
}
