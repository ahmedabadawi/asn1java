package io.github.ahmedabadawi.asn1java.plugin;

import io.github.ahmedabadawi.asn1java.core.Asn1Spec;
import io.github.ahmedabadawi.asn1java.core.ast.ImportedTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ModuleNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;
import io.github.ahmedabadawi.asn1java.core.codegen.Asn1CodeGenerator;
import io.github.ahmedabadawi.asn1java.core.codegen.Asn1CodeWriter;
import io.github.ahmedabadawi.asn1java.core.codegen.JavaPackage;
import io.github.ahmedabadawi.asn1java.core.exception.Asn1SemanticException;
import io.github.ahmedabadawi.asn1java.core.exception.Asn1SyntaxException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class Asn1CodeGenPlugin extends AbstractMojo {

  @Parameter(required = true)
  List<SpecFile> specFiles;

  @Parameter(required = true)
  JavaPackage basePackage;

  @Parameter(defaultValue = "${project.build.directory}/generated-sources/asn1java")
  File outputDirectory;

  @Parameter(defaultValue = "${project}", readonly = true)
  MavenProject project;

  private record ParsedSpec(SpecFile specFile, ModuleNode module, JavaPackage targetPackage) {
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    warnIfRuntimeMissing();
    project.addCompileSourceRoot(outputDirectory.getAbsolutePath());

    List<ParsedSpec> parsedSpecs = new ArrayList<>();
    for (SpecFile specFile : specFiles) {
      if (specFile.file == null) {
        throw new MojoExecutionException("specFile is missing a required <file>");
      }
      File file = specFile.file;
      if (specFile.packageName == null && basePackage == null) {
        throw new MojoExecutionException(
            "%s has no packageName set and no basePackage is configured".formatted(file));
      }

      getLog().info("Parsing spec: %s".formatted(file));
      String source;
      try {
        source = Files.readString(file.toPath());
      } catch (IOException e) {
        throw new MojoExecutionException("Failed to read spec file: %s".formatted(file), e);
      }

      var module = parse(file, source);
      JavaPackage targetPackage = specFile.packageName != null ? specFile.packageName : basePackage;
      parsedSpecs.add(new ParsedSpec(specFile, module, targetPackage));
    }

    // Merge function keeps the first spec seen for a given module name: two spec files may
    // legitimately share a module name (e.g. the same module compiled into two target
    // packages), which is unrelated to — and must not break — cross-module IMPORTS resolution.
    Map<String, JavaPackage> modulePackages = parsedSpecs.stream()
        .collect(Collectors.toMap(spec -> spec.module().name(), ParsedSpec::targetPackage,
            (first, second) -> first));
    Map<String, Set<String>> moduleTypeNames = parsedSpecs.stream()
        .collect(Collectors.toMap(spec -> spec.module().name(), spec -> spec.module().types()
            .stream()
            .map(TypeAssignmentNode::name)
            .collect(Collectors.toSet()), (first, second) -> first));

    for (ParsedSpec parsedSpec : parsedSpecs) {
      for (ImportedTypeNode imported : parsedSpec.module().imports()) {
        Set<String> exportedNames = moduleTypeNames.get(imported.moduleName());
        if (exportedNames == null) {
          throw new MojoFailureException(
              "%s imports from unknown module '%s'"
                  .formatted(parsedSpec.specFile().file, imported.moduleName()));
        }
        if (!exportedNames.contains(imported.typeName())) {
          throw new MojoFailureException(
              "%s imports unknown type '%s' from module '%s'"
                  .formatted(parsedSpec.specFile().file, imported.typeName(),
                      imported.moduleName()));
        }
      }
    }

    for (ParsedSpec parsedSpec : parsedSpecs) {
      getLog().info("Generating sources from: %s".formatted(parsedSpec.specFile().file));
      var generator = new Asn1CodeGenerator(parsedSpec.targetPackage(),
          moduleName -> modulePackages.containsKey(moduleName)
              ? modulePackages.get(moduleName).child(moduleName.toLowerCase()).value()
              : null);
      var files = generator.generate(parsedSpec.module());

      try {
        Asn1CodeWriter.writeTo(files, outputDirectory.toPath());
      } catch (IOException e) {
        throw new MojoExecutionException("Failed to write generated sources", e);
      }

      files.forEach(f -> getLog().info("Generated: %s.java".formatted(f.typeSpec().name())));
    }
  }

  private void warnIfRuntimeMissing() {
    boolean declared =
        project.getDependencies().stream()
            .anyMatch(d -> "asn1java-runtime".equals(d.getArtifactId()));
    if (!declared) {
      getLog()
          .warn(
              "Generated code requires asn1java-runtime but it was not found in the project dependencies.");
      getLog().warn("Add the following to your pom.xml:");
      getLog().warn("  <dependency>");
      getLog().warn("    <groupId>io.github.ahmedabadawi</groupId>");
      getLog().warn("    <artifactId>asn1java-runtime</artifactId>");
      getLog().warn("    <version>...</version>");
      getLog().warn("  </dependency>");
    }
  }

  private ModuleNode parse(File specFile, String source) throws MojoFailureException {
    try {
      return Asn1Spec.parse(source);
    } catch (Asn1SyntaxException e) {
      throw new MojoFailureException(
          "Syntax error in %s at line %d:%d — %s"
              .formatted(specFile, e.line(), e.charPosition(), e.getMessage()),
          e);
    } catch (Asn1SemanticException e) {
      throw new MojoFailureException(
          "Semantic error in %s: %s".formatted(specFile, e.getMessage()), e);
    }
  }
}
