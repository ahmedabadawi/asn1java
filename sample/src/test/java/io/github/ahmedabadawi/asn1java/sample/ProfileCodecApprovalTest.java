package io.github.ahmedabadawi.asn1java.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ahmedabadawi.asn1java.sample.profilemodule.Profile;
import io.github.ahmedabadawi.asn1java.sample.profilemodule.ProfileCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ProfileCodecApprovalTest {

  private static final ProfileCodec CODEC = new ProfileCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/profile");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encode_WhenValuesEqualDefaults_ShouldMatchGoldenHex() throws IOException {
    // Given
    var profile = new Profile(1, 1, "anonymous");

    // When
    var hex = HEX.formatHex(CODEC.encode(profile));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encode_WhenValuesDifferFromDefaults_ShouldMatchGoldenHex() throws IOException {
    // Given
    var profile = new Profile(2, 0, "Alice");

    // When
    var hex = HEX.formatHex(CODEC.encode(profile));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decode_WhenValuesEqualDefaultsEncoded_ShouldReturnProfileWithDefaults() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var profile = CODEC.decode(bytes);

    // Then
    assertThat(profile).isEqualTo(new Profile(1, 1, "anonymous"));
  }

  @Test
  void decode_WhenValuesDifferFromDefaultsEncoded_ShouldReturnProfileWithValues() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var profile = CODEC.decode(bytes);

    // Then
    assertThat(profile).isEqualTo(new Profile(2, 0, "Alice"));
  }

  @Test
  void construct_WhenIdExceedsMax_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Profile(999, 1, "anonymous"));

    // Then
    assertThat(thrown).hasMessageContaining("id must be <= 255");
  }
}
