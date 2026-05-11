package io.github.ahmedabadawi.asn1java.plugin;

import io.github.ahmedabadawi.asn1java.core.Asn1Spec;
import io.github.ahmedabadawi.asn1java.core.codegen.Asn1CodeGenerator;
import io.github.ahmedabadawi.asn1java.core.codegen.Asn1CodeWriter;
import io.github.ahmedabadawi.asn1java.core.exception.Asn1SemanticException;
import io.github.ahmedabadawi.asn1java.core.ast.ModuleNode;
import io.github.ahmedabadawi.asn1java.core.exception.Asn1SyntaxException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Mojo(
    name = "generate",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    threadSafe = true)
public class Asn1CodeGenPlugin extends AbstractMojo {

    @Parameter(required = true)
    List<File> specFiles;

    @Parameter(required = true)
    String basePackage;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/asn1java")
    File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        warnIfRuntimeMissing();
        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());

        var generator = new Asn1CodeGenerator(basePackage);

        for (File specFile : specFiles) {
            getLog().info("Generating sources from: %s".formatted(specFile));
            String source;
            try {
                source = Files.readString(specFile.toPath());
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to read spec file: %s".formatted(specFile), e);
            }

            var module = parse(specFile, source);
            var files = generator.generate(module);

            try {
                Asn1CodeWriter.writeTo(files, outputDirectory.toPath());
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to write generated sources", e);
            }

            files.forEach(f -> getLog().info("Generated: %s.java".formatted(f.typeSpec().name())));
        }
    }

    private void warnIfRuntimeMissing() {
        boolean declared = project.getDependencies().stream()
                .anyMatch(d -> "asn1java-runtime".equals(d.getArtifactId()));
        if (!declared) {
            getLog().warn("Generated code requires asn1java-runtime but it was not found in the project dependencies.");
            getLog().warn("Add the following to your pom.xml:");
            getLog().warn("  <dependency>");
            getLog().warn("    <groupId>io.github.ahmedabadawi</groupId>");
            getLog().warn("    <artifactId>asn1java-runtime</artifactId>");
            getLog().warn("    <version>...</version>");
            getLog().warn("  </dependency>");
        }
    }

    private ModuleNode parse(File specFile, String source)
            throws MojoFailureException {
        try {
            return Asn1Spec.parse(source);
        } catch (Asn1SyntaxException e) {
            throw new MojoFailureException(
                    "Syntax error in %s at line %d:%d — %s".formatted(specFile, e.line(), e.charPosition(), e.getMessage()), e);
        } catch (Asn1SemanticException e) {
            throw new MojoFailureException(
                    "Semantic error in %s: %s".formatted(specFile, e.getMessage()), e);
        }
    }
}
