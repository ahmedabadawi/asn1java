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
        String pkg = basePackage + "." + module.name().toLowerCase();
        List<JavaFile> files = new ArrayList<>();
        for (TypeAssignmentNode ta : module.types()) {
            files.add(ModelGenerator.generate(pkg, ta));
            files.add(CodecGenerator.generate(pkg, ta));
        }
        return files;
    }
}
