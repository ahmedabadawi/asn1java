package io.github.ahmedabadawi.asn1java.core.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.ahmedabadawi.asn1java.core.ast.BitStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.BooleanTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.ChoiceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.Ia5StringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.VisibleStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.NullTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.EnumeratedTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.FieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.IntegerTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.MinBound;
import io.github.ahmedabadawi.asn1java.core.ast.OctetStringTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceFieldNode;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceOfTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.SequenceTypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeAssignmentNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeNode;
import io.github.ahmedabadawi.asn1java.core.ast.TypeReferenceNode;
import io.github.ahmedabadawi.asn1java.core.ast.Utf8StringTypeNode;

import javax.lang.model.element.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

final class ModelGenerator {

  private ModelGenerator() {
  }

  private static final ClassName STRING = ClassName.get("java.lang", "String");
  private static final com.palantir.javapoet.ArrayTypeName BYTE_ARRAY =
      com.palantir.javapoet.ArrayTypeName.of(TypeName.BYTE);

  static JavaFile generate(String targetPackage, TypeAssignmentNode typeAssignment,
      Function<String, String> typePackageResolver) {
    TypeSpec record = switch (typeAssignment.type()) {
      case SequenceTypeNode seq -> buildSequenceRecord(targetPackage, typeAssignment.name(), seq,
          CodecGenerator.collectFields(typeAssignment), typePackageResolver);
      case ChoiceTypeNode choice -> buildChoiceInterface(targetPackage, typeAssignment.name(),
          choice, typePackageResolver);
      case IntegerTypeNode ignored -> buildIntegerWrapperRecord(typeAssignment.name(),
          CodecGenerator.collectFields(typeAssignment).get(0));
      case BooleanTypeNode ignored -> buildBooleanWrapperRecord(typeAssignment.name());
      case Utf8StringTypeNode ignored -> buildUtf8StringWrapperRecord(typeAssignment.name(),
          CodecGenerator.collectFields(typeAssignment).get(0));
      case OctetStringTypeNode ignored -> buildByteArrayWrapperRecord(typeAssignment.name(),
          CodecGenerator.collectFields(typeAssignment).get(0));
      case BitStringTypeNode ignored -> buildByteArrayWrapperRecord(typeAssignment.name(),
          CodecGenerator.collectFields(typeAssignment).get(0));
      case NullTypeNode ignored -> buildEmptyRecord(typeAssignment.name());
      case Ia5StringTypeNode ignored -> buildUtf8StringWrapperRecord(typeAssignment.name(),
          CodecGenerator.collectFields(typeAssignment).get(0));
      case VisibleStringTypeNode ignored -> buildUtf8StringWrapperRecord(typeAssignment.name(),
          CodecGenerator.collectFields(typeAssignment).get(0));
      case EnumeratedTypeNode ignored -> buildIntegerWrapperRecord(typeAssignment.name(),
          CodecGenerator.collectFields(typeAssignment).get(0));
      case SequenceOfTypeNode sequenceOfType -> buildSequenceOfWrapperRecord(targetPackage,
          typeAssignment.name(), sequenceOfType, CodecGenerator.collectFields(typeAssignment).get(0),
          typePackageResolver);
      case TypeReferenceNode ignored ->
          throw new IllegalArgumentException(
              "top-level TypeReferenceNode is not a valid type assignment body: "
                  + typeAssignment.name());
    };
    return JavaFile.builder(targetPackage, record).build();
  }

  private static TypeSpec buildSequenceRecord(String targetPackage, String name,
      SequenceTypeNode seq, List<CodecGenerator.EncodedField> fields,
      Function<String, String> typePackageResolver) {
    MethodSpec.Builder ctorBuilder = MethodSpec.compactConstructorBuilder()
        .addModifiers(Modifier.PUBLIC);
    Iterator<CodecGenerator.EncodedField> fieldIterator = fields.iterator();
    for (SequenceFieldNode field : seq.fields()) {
      if (field.type() instanceof NullTypeNode) {
        continue;
      }
      TypeName javaType = resolveFieldJavaType(targetPackage, field.type(), typePackageResolver);
      if (field.optional()) {
        javaType = javaType.box();
      }
      ctorBuilder.addParameter(javaType, CodegenUtils.toJavaFieldName(field.name()));
      CodecGenerator.addFieldValidation(ctorBuilder, fieldIterator.next());
    }
    return TypeSpec.recordBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .recordConstructor(ctorBuilder.build())
        .build();
  }

  private static TypeName resolveFieldJavaType(String targetPackage, TypeNode type,
      Function<String, String> typePackageResolver) {
    return switch (type) {
      case IntegerTypeNode intType ->
          intType.constraint() != null && intType.constraint().lowerBound() instanceof MinBound
              ? TypeName.LONG : TypeName.INT;
      case BooleanTypeNode ignored -> TypeName.BOOLEAN;
      case Utf8StringTypeNode ignored -> STRING;
      case OctetStringTypeNode ignored -> BYTE_ARRAY;
      case BitStringTypeNode ignored -> BYTE_ARRAY;
      case NullTypeNode ignored ->
          throw new IllegalStateException("null type should have been skipped");
      case Ia5StringTypeNode ignored -> STRING;
      case VisibleStringTypeNode ignored -> STRING;
      case SequenceTypeNode ignored ->
          throw new IllegalArgumentException("nested SEQUENCE not supported in record generator");
      case ChoiceTypeNode ignored ->
          throw new IllegalArgumentException("nested CHOICE not supported in record generator");
      case EnumeratedTypeNode ignored -> TypeName.INT;
      case SequenceOfTypeNode sequenceOfType -> ParameterizedTypeName.get(ClassName.get(List.class),
          resolveFieldJavaType(targetPackage, sequenceOfType.elementType(), typePackageResolver)
              .box());
      case TypeReferenceNode ref ->
          ClassName.get(typePackageResolver.apply(ref.typeName()), ref.typeName());
    };
  }

  private static TypeSpec buildChoiceInterface(String targetPackage, String name,
      ChoiceTypeNode choice, Function<String, String> typePackageResolver) {
    ClassName selfClassName = ClassName.get(targetPackage, name);
    TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(name)
        .addModifiers(Modifier.PUBLIC, Modifier.SEALED);
    for (FieldNode alternative : choice.alternatives()) {
      String variantName = CodegenUtils.toJavaClassName(alternative.name());
      MethodSpec.Builder ctorBuilder = MethodSpec.compactConstructorBuilder()
          .addModifiers(Modifier.PUBLIC);
      if (!(alternative.type() instanceof NullTypeNode)) {
        TypeName payloadType =
            resolveFieldJavaType(targetPackage, alternative.type(), typePackageResolver);
        ctorBuilder.addParameter(payloadType, "value");
        if (alternative.type() instanceof TypeReferenceNode) {
          ctorBuilder.beginControlFlow("if (value == null)")
              .addStatement("throw new $T($S)", IllegalArgumentException.class,
                  "value must not be null")
              .endControlFlow();
        }
      }
      interfaceBuilder.addType(TypeSpec.recordBuilder(variantName)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .addSuperinterface(selfClassName)
          .recordConstructor(ctorBuilder.build())
          .build());
    }
    return interfaceBuilder.build();
  }

  private static TypeSpec buildIntegerWrapperRecord(String name,
      CodecGenerator.EncodedField field) {
    MethodSpec.Builder ctorBuilder = MethodSpec.compactConstructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(TypeName.INT, "value");
    CodecGenerator.addFieldValidation(ctorBuilder, field);
    return TypeSpec.recordBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .recordConstructor(ctorBuilder.build())
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

  private static TypeSpec buildUtf8StringWrapperRecord(String name,
      CodecGenerator.EncodedField field) {
    MethodSpec.Builder ctorBuilder = MethodSpec.compactConstructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(STRING, "value");
    CodecGenerator.addFieldValidation(ctorBuilder, field);
    return TypeSpec.recordBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .recordConstructor(ctorBuilder.build())
        .build();
  }

  private static TypeSpec buildEmptyRecord(String name) {
    MethodSpec ctor = MethodSpec.constructorBuilder().build();
    return TypeSpec.recordBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .recordConstructor(ctor)
        .build();
  }

  private static TypeSpec buildByteArrayWrapperRecord(String name,
      CodecGenerator.EncodedField field) {
    MethodSpec.Builder ctorBuilder = MethodSpec.compactConstructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(BYTE_ARRAY, "value");
    CodecGenerator.addFieldValidation(ctorBuilder, field);
    return TypeSpec.recordBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .recordConstructor(ctorBuilder.build())
        .build();
  }

  private static TypeSpec buildSequenceOfWrapperRecord(String targetPackage, String name,
      SequenceOfTypeNode sequenceOfType, CodecGenerator.EncodedField field,
      Function<String, String> typePackageResolver) {
    TypeName elementType =
        resolveFieldJavaType(targetPackage, sequenceOfType.elementType(), typePackageResolver);
    TypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), elementType.box());
    MethodSpec.Builder ctorBuilder = MethodSpec.compactConstructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(listType, "value");
    CodecGenerator.addFieldValidation(ctorBuilder, field);
    return TypeSpec.recordBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .recordConstructor(ctorBuilder.build())
        .build();
  }
}
