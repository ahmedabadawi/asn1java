package io.github.ahmedabadawi.asn1java.core.ast;

import java.util.List;

public record SequenceTypeNode(List<SequenceFieldNode> fields) implements TypeNode {
}
