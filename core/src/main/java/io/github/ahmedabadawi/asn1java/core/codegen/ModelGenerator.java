package io.github.ahmedabadawi.asn1java.core.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.ahmedabadawi.asn1java.core.ast.BooleanTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.EnumeratedTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.FieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MinBound;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;
import io.github.ahmedabadawi.asn1java.core.ast.Utf8StringTypeNode;

import javax.lang.model.element.Modifier;

final class ModelGenerator {

  private ModelGenerator() {
  }

  private static final ClassName STRING = ClassName.get("java.lang", "String");

  static JavaFile generate(String targetPackage, TypeAssignmentNode typeAssignment) {
    TypeSpec record = switch (typeAssignment.type()) {
      case SequenceTypeNode seq -> buildSequenceRecord(typeAssignment.name(), seq);
      case IntegerTypeNode ignored -> buildIntegerWrapperRecord(typeAssignment.name());
      case BooleanTypeNode ignored -> buildBooleanWrapperRecord(typeAssignment.name());
      case Utf8StringTypeNode ignored -> buildUtf8StringWrapperRecord(typeAssignment.name());
      case EnumeratedTypeNode ignored -> buildIntegerWrapperRecord(typeAssignment.name());
    };
    return JavaFile.builder(targetPackage, record).build();
  }

  private static TypeSpec buildSequenceRecord(String name, SequenceTypeNode seq) {
    MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder();
    for (FieldNode field : seq.fields()) {
      TypeName javaType = switch (field.type()) {
        case IntegerTypeNode intType ->
            intType.constraint() != null && intType.constraint().lowerBound() instanceof MinBound
                ? TypeName.LONG : TypeName.INT;
        case BooleanTypeNode ignored -> TypeName.BOOLEAN;
        case Utf8StringTypeNode ignored -> STRING;
        case SequenceTypeNode ignored ->
            throw new IllegalArgumentException("nested SEQUENCE not supported in record generator");
        case EnumeratedTypeNode ignored -> TypeName.INT;
      };
      ctorBuilder.addParameter(javaType, field.name());
    }
    return TypeSpec.recordBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .recordConstructor(ctorBuilder.build())
        .build();
  }

  private static TypeSpec buildIntegerWrapperRecord(String name) {
    MethodSpec ctor = MethodSpec.constructorBuilder()
        .addParameter(TypeName.INT, "value")
        .build();
    return TypeSpec.recordBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .recordConstructor(ctor)
        .build();
  }

  private static TypeSpec buildBooleanWrapperRecord(String name) {
    MethodSpec ctor = MethodSpec.constructorBuilder()
        .addParameter(TypeName.BOOLEAN, "value")
        .build();
    return TypeSpec.recordBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .recordConstructor(ctor)
        .build();
  }

  private static TypeSpec buildUtf8StringWrapperRecord(String name) {
    MethodSpec ctor = MethodSpec.constructorBuilder()
        .addParameter(STRING, "value")
        .build();
    return TypeSpec.recordBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .recordConstructor(ctor)
        .build();
  }
}
