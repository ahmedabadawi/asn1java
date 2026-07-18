package io.github.ahmedabadawi.asn1java.core.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.Test;

class JavaPackageTest {

  @Test
  void construct_WhenValueIsValidDottedName_ShouldSucceed() {
    // Given
    var value = "io.github.ahmedabadawi.asn1java";

    // When
    var javaPackage = new JavaPackage(value);

    // Then
    assertThat(javaPackage.value()).isEqualTo(value);
  }

  @Test
  void construct_WhenValueIsBlank_ShouldThrowIllegalArgumentException() {
    // Given / When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new JavaPackage("  "));

    // Then
    assertThat(thrown).hasMessageContaining("must not be blank");
  }

  @Test
  void construct_WhenSegmentStartsWithDigit_ShouldThrowIllegalArgumentException() {
    // Given / When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new JavaPackage("io.2fast"));

    // Then
    assertThat(thrown).hasMessageContaining("2fast");
  }

  @Test
  void construct_WhenSegmentIsReservedWord_ShouldThrowIllegalArgumentException() {
    // Given / When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new JavaPackage("io.class.example"));

    // Then
    assertThat(thrown).hasMessageContaining("reserved word");
  }

  @Test
  void construct_WhenValueHasTrailingDot_ShouldThrowIllegalArgumentException() {
    // Given / When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new JavaPackage("io.example."));

    // Then
    assertThat(thrown).hasMessageContaining("not a valid Java package name");
  }

  @Test
  void child_WhenGivenSimpleName_ShouldAppendAsNewSegment() {
    // Given
    var javaPackage = new JavaPackage("io.example");

    // When
    var child = javaPackage.child("versioninfo");

    // Then
    assertThat(child.value()).isEqualTo("io.example.versioninfo");
  }

  @Test
  void child_WhenSimpleNameIsInvalid_ShouldThrowIllegalArgumentException() {
    // Given
    var javaPackage = new JavaPackage("io.example");

    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> javaPackage.child("class"));

    // Then
    assertThat(thrown).hasMessageContaining("reserved word");
  }
}
