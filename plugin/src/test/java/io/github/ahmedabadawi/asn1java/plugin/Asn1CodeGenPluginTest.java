package io.github.ahmedabadawi.asn1java.plugin;

import org.apache.maven.model.Dependency;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Asn1CodeGenPluginTest {

    private static final String SIMPLE_ASN = """
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
        var mojo = new Asn1CodeGenPlugin();
        mojo.specFiles = List.of(specFile);
        mojo.basePackage = basePackage;
        mojo.outputDirectory = outputDir;
        mojo.project = new MavenProject() {
            @Override
            public void addCompileSourceRoot(String path) { /* no-op in tests */ }

            @Override
            public List<Dependency> getDependencies() { return dependencies; }
        };
        return mojo;
    }

    private static List<String> captureWarnings(Asn1CodeGenPlugin mojo) throws Exception {
        var warnings = new ArrayList<String>();
        mojo.setLog(new DefaultLog(new ConsoleLogger(Logger.LEVEL_WARN, "test")) {
            @Override
            public void warn(CharSequence content) { warnings.add(content.toString()); }
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

        Path versioninfoDir = outputDir.toPath().resolve("io/example/versioninfo");
        assertThat(versioninfoDir.resolve("Version.java")).exists();
        assertThat(versioninfoDir.resolve("VersionCodec.java")).exists();
    }

    @Test
    void execute_generatedModelContainsRecord(@TempDir Path tmp) throws Exception {
        var specFile = tmp.resolve("simple.asn").toFile();
        Files.writeString(specFile.toPath(), SIMPLE_ASN);
        var outputDir = tmp.resolve("generated").toFile();

        mojo(specFile, outputDir, "io.example").execute();

        String content = Files.readString(
                outputDir.toPath().resolve("io/example/versioninfo/Version.java"));
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
        var mojo = mojo(specFile, tmp.resolve("generated").toFile(), "io.example",
                List.of(runtimeDependency()));

        var warnings = captureWarnings(mojo);

        assertThat(warnings).noneMatch(w -> w.contains("asn1java-runtime"));
    }
}
