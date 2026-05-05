package io.github.ahmedabadawi.asn1java.core.ast;

import java.util.List;

public record ModuleNode(String name, List<TypeAssignmentNode> types) {
}
