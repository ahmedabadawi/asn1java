package io.github.ahmedabadawi.asn1java.core.codegen;

import java.util.Set;

public record JavaPackage(String value) {

  private static final Set<String> RESERVED_WORDS = Set.of(
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
      "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
      "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
      "interface", "long", "native", "new", "package", "private", "protected", "public",
      "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
      "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false",
      "null");

  public JavaPackage {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Java package name must not be blank");
    }
    for (String segment : value.split("\\.", -1)) {
      if (!segment.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
        throw new IllegalArgumentException(
            "'%s' is not a valid Java package name — invalid segment '%s'"
                .formatted(value, segment));
      }
      if (RESERVED_WORDS.contains(segment)) {
        throw new IllegalArgumentException(
            "'%s' is not a valid Java package name — '%s' is a reserved word"
                .formatted(value, segment));
      }
    }
  }

  public JavaPackage child(String simpleName) {
    return new JavaPackage(value + "." + simpleName);
  }
}
