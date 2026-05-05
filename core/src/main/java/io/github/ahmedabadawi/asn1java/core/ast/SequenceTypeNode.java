package io.github.ahmedabadawi.asn1java.core.ast;

import java.util.List;

public record SequenceTypeNode(List<FieldNode> fields) implements TypeNode {
}
