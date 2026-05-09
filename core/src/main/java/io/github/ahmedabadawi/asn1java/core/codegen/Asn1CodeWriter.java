package io.github.ahmedabadawi.asn1java.core.codegen;

import com.palantir.javapoet.JavaFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class Asn1CodeWriter {

    private Asn1CodeWriter() {}

    public static void writeTo(List<JavaFile> files, Path outputDir) throws IOException {
        for (JavaFile file : files) {
            file.writeTo(outputDir);
        }
    }
}
