package io.github.ahmedabadawi.asn1java.core.ast;

import java.util.List;

public record EnumeratedTypeNode(List<String> values) implements TypeNode {
}
