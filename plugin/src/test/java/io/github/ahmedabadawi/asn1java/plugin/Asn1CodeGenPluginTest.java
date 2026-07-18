package io.github.ahmedabadawi.asn1java.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ahmedabadawi.asn1java.core.codegen.JavaPackage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Asn1CodeGenPluginTest {

  private static final String SIMPLE_ASN =
      """
      VersionInfo DEFINITIONS AUTOMATIC TAGS ::= BEGIN
          Version ::= SEQUENCE {
              major  INTEGER (0..MAX),
              minor  INTEGER (0..MAX)
          }
      END
      """;

  private Asn1CodeGenPlugin mojo(File specFile, File outputDir, String basePackage) {
    return mojo(specFile, outputDir, basePackage, List.of());
  }

  private Asn1CodeGenPlugin mojo(File specFile, File outputDir, String basePackage,
                                 List<Dependency> dependencies) {
    var spec = new SpecFile();
    spec.file = specFile;
    return mojo(List.of(spec), outputDir, basePackage, dependencies);
  }

  private Asn1CodeGenPlugin mojo(List<SpecFile> specFiles, File outputDir, String basePackage,
                                 List<Dependency> dependencies) {

    var mojo = new Asn1CodeGenPlugin();
    mojo.specFiles = specFiles;
    mojo.basePackage = new JavaPackage(basePackage);
    mojo.outputDirectory = outputDir;
    mojo.project =
        new MavenProject() {
          @Override
          public void addCompileSourceRoot(String path) {
            /* no-op in tests */
          }

          @Override
          public List<Dependency> getDependencies() {
            return dependencies;
          }
        };
    return mojo;
  }

  private static List<String> captureWarnings(Asn1CodeGenPlugin mojo) throws Exception {
    var warnings = new ArrayList<String>();
    mojo.setLog(
        new DefaultLog(new ConsoleLogger(Logger.LEVEL_WARN, "test")) {
          @Override
          public void warn(CharSequence content) {
            warnings.add(content.toString());
          }
        });
    mojo.execute();
    return warnings;
  }

  private static Dependency runtimeDependency() {
    var dep = new Dependency();
    dep.setGroupId("io.github.ahmedabadawi");
    dep.setArtifactId("asn1java-runtime");
    dep.setVersion("0-SNAPSHOT");
    return dep;
  }

  @Test
  void execute_generatesModelAndCodecForSimpleSpec(@TempDir Path tmp) throws Exception {
    var specFile = tmp.resolve("simple.asn").toFile();
    Files.writeString(specFile.toPath(), SIMPLE_ASN);
    var outputDir = tmp.resolve("generated").toFile();

    mojo(specFile, outputDir, "io.example").execute();

    Path versionInfoDir = outputDir.toPath().resolve("io/example/versioninfo");
    assertThat(versionInfoDir.resolve("Version.java")).exists();
    assertThat(versionInfoDir.resolve("VersionCodec.java")).exists();
  }

  @Test
  void execute_generatedModelContainsRecord(@TempDir Path tmp) throws Exception {
    var specFile = tmp.resolve("simple.asn").toFile();
    Files.writeString(specFile.toPath(), SIMPLE_ASN);
    var outputDir = tmp.resolve("generated").toFile();

    mojo(specFile, outputDir, "io.example").execute();

    String content =
        Files.readString(outputDir.toPath().resolve("io/example/versioninfo/Version.java"));
    assertThat(content).contains("public record Version(");
    assertThat(content).contains("int major");
    assertThat(content).contains("int minor");
  }

  @Test
  void execute_generatedCodecContainsEncodeAndDecode(@TempDir Path tmp) throws Exception {
    var specFile = tmp.resolve("simple.asn").toFile();
    Files.writeString(specFile.toPath(), SIMPLE_ASN);
    var outputDir = tmp.resolve("generated").toFile();

    mojo(specFile, outputDir, "io.example").execute();

    String content = Files.readString(
        outputDir.toPath().resolve("io/example/versioninfo/VersionCodec.java"));
    assertThat(content).contains("public byte[] encode(");
    assertThat(content).contains("public Version decode(");
  }

  @Test
  void execute_specFileHasPackageOverride_generatesIntoOverridePackage(@TempDir Path tmp)
      throws Exception {
    var specFile = tmp.resolve("simple.asn").toFile();
    Files.writeString(specFile.toPath(), SIMPLE_ASN);
    var outputDir = tmp.resolve("generated").toFile();
    var spec = new SpecFile();
    spec.file = specFile;
    spec.packageName = new JavaPackage("io.override");

    mojo(List.of(spec), outputDir, "io.example", List.of()).execute();

    Path versionInfoDir = outputDir.toPath().resolve("io/override/versioninfo");
    assertThat(versionInfoDir.resolve("Version.java")).exists();
    assertThat(versionInfoDir.resolve("VersionCodec.java")).exists();
    assertThat(outputDir.toPath().resolve("io/example")).doesNotExist();
  }

  @Test
  void execute_multipleSpecFilesWithMixedPackages_generatesEachIntoItsOwnPackage(
      @TempDir Path tmp) throws Exception {
    var defaultSpecFile = tmp.resolve("simple.asn").toFile();
    Files.writeString(defaultSpecFile.toPath(), SIMPLE_ASN);
    var overrideSpecFile = tmp.resolve("simple-2.asn").toFile();
    Files.writeString(overrideSpecFile.toPath(), SIMPLE_ASN);
    var outputDir = tmp.resolve("generated").toFile();

    var defaultSpec = new SpecFile();
    defaultSpec.file = defaultSpecFile;
    var overrideSpec = new SpecFile();
    overrideSpec.file = overrideSpecFile;
    overrideSpec.packageName = new JavaPackage("io.override");

    mojo(List.of(defaultSpec, overrideSpec), outputDir, "io.example", List.of()).execute();

    assertThat(outputDir.toPath().resolve("io/example/versioninfo/Version.java")).exists();
    assertThat(outputDir.toPath().resolve("io/override/versioninfo/Version.java")).exists();
  }

  @Test
  void execute_outputDirectoryIsCreatedIfAbsent(@TempDir Path tmp) throws Exception {
    var specFile = tmp.resolve("simple.asn").toFile();
    Files.writeString(specFile.toPath(), SIMPLE_ASN);
    var outputDir = tmp.resolve("deep/nested/generated").toFile();

    mojo(specFile, outputDir, "io.example").execute();

    assertThat(outputDir).isDirectory();
  }

  @Test
  void execute_syntaxErrorInSpec_throwsMojoFailureException(@TempDir Path tmp) throws Exception {
    var specFile = tmp.resolve("bad.asn").toFile();
    Files.writeString(specFile.toPath(), "this is not valid ASN.1");
    var outputDir = tmp.resolve("generated").toFile();

    assertThatThrownBy(() -> mojo(specFile, outputDir, "io.example").execute())
        .isInstanceOf(MojoFailureException.class)
        .hasMessageContaining("Syntax error");
  }

  @Test
  void execute_runtimeDependencyAbsent_emitsWarning(@TempDir Path tmp) throws Exception {
    var specFile = tmp.resolve("simple.asn").toFile();
    Files.writeString(specFile.toPath(), SIMPLE_ASN);
    var mojo = mojo(specFile, tmp.resolve("generated").toFile(), "io.example", List.of());

    var warnings = captureWarnings(mojo);

    assertThat(warnings).anyMatch(w -> w.contains("asn1java-runtime"));
  }

  @Test
  void execute_runtimeDependencyPresent_noWarning(@TempDir Path tmp) throws Exception {
    var specFile = tmp.resolve("simple.asn").toFile();
    Files.writeString(specFile.toPath(), SIMPLE_ASN);
    var mojo = mojo(
        specFile,
        tmp.resolve("generated").toFile(),
        "io.example",
        List.of(runtimeDependency()));

    var warnings = captureWarnings(mojo);

    assertThat(warnings).noneMatch(w -> w.contains("asn1java-runtime"));
  }
}
