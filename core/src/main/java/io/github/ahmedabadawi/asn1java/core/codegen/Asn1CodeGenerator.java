package io.github.ahmedabadawi.asn1java.core.codegen;

import com.palantir.javapoet.JavaFile;
import io.github.ahmedabadawi.asn1java.core.ast.ModuleNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;

import java.util.ArrayList;
import java.util.List;

public final class Asn1CodeGenerator {

  private final String basePackage;

  public Asn1CodeGenerator(String basePackage) {
    this.basePackage = basePackage;
  }

  public List<JavaFile> generate(ModuleNode module) {
    String targetPackage = basePackage + "." + module.name().toLowerCase();
    return module.types().stream()
        .map(typeAssignment -> generate(targetPackage, typeAssignment))
        .flatMap(List::stream)
        .toList();
  }

  private List<JavaFile> generate(String targetPackage, TypeAssignmentNode typeAssignment) {
    return List.of(
        ModelGenerator.generate(targetPackage, typeAssignment),
        CodecGenerator.generate(targetPackage, typeAssignment));
  }
}
