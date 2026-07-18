package io.github.ahmedabadawi.asn1java.core.codegen;

final class CodegenUtils {

  private CodegenUtils() {
  }

  static String toJavaFieldName(String asn1Name) {
    if (!asn1Name.contains("-")) {
      return asn1Name;
    }
    var parts = asn1Name.split("-");
    var result = new StringBuilder(parts[0]);
    for (int index = 1; index < parts.length; index++) {
      result.append(Character.toUpperCase(parts[index].charAt(0))).append(parts[index].substring(1));
    }
    return result.toString();
  }

  static String toJavaClassName(String asn1Name) {
    String fieldName = toJavaFieldName(asn1Name);
    return Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
  }
}
