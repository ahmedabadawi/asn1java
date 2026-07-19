package io.github.ahmedabadawi.asn1java.core.ast;

import java.util.List;

public record ModuleNode(String name, List<TypeAssignmentNode> types,
    List<ImportedTypeNode> imports) {

  public ModuleNode(String name, List<TypeAssignmentNode> types) {
    this(name, types, List.of());
  }
}
