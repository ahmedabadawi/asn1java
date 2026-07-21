package io.github.ahmedabadawi.asn1java.core.codegen;

import com.palantir.javapoet.JavaFile;
import io.github.ahmedabadawi.asn1java.core.ast.ImportedTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ModuleNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Asn1CodeGenerator {

  private final JavaPackage basePackage;
  private final Function<String, String> importedModulePackageResolver;

  public Asn1CodeGenerator(JavaPackage basePackage) {
    this(basePackage, moduleName -> null);
  }

  public Asn1CodeGenerator(JavaPackage basePackage,
      Function<String, String> importedModulePackageResolver) {
    this.basePackage = basePackage;
    this.importedModulePackageResolver = importedModulePackageResolver;
  }

  public List<JavaFile> generate(ModuleNode module) {
    String targetPackage = basePackage.child(module.name().toLowerCase()).value();
    Set<String> localTypeNames = module.types().stream()
        .map(TypeAssignmentNode::name)
        .collect(Collectors.toSet());
    Function<String, String> typePackageResolver = typeName -> localTypeNames.contains(typeName)
        ? targetPackage
        : resolveImportedPackage(module, typeName);
    return module.types().stream()
        .map(typeAssignment -> generate(targetPackage, typeAssignment, typePackageResolver))
        .flatMap(List::stream)
        .toList();
  }

  private String resolveImportedPackage(ModuleNode module, String typeName) {
    ImportedTypeNode imported = module.imports().stream()
        .filter(candidate -> candidate.typeName().equals(typeName))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Type reference '%s' is neither declared in module '%s' nor imported"
                .formatted(typeName, module.name())));
    String importedPackage = importedModulePackageResolver.apply(imported.moduleName());
    if (importedPackage == null) {
      throw new IllegalArgumentException(
          "Module '%s' imports '%s' from '%s', but no cross-module package resolution was configured"
              .formatted(module.name(), typeName, imported.moduleName()));
    }
    return importedPackage;
  }

  private List<JavaFile> generate(String targetPackage, TypeAssignmentNode typeAssignment,
      Function<String, String> typePackageResolver) {
    return List.of(
        ModelGenerator.generate(targetPackage, typeAssignment, typePackageResolver),
        CodecGenerator.generate(targetPackage, typeAssignment, typePackageResolver));
  }
}
