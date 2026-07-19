package io.github.ahmedabadawi.asn1java.handwritten.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class SettingsCodecTest {

  private static final SettingsCodec CODEC = new SettingsCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/settings");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encode_WhenValuesEqualDefaults_ShouldMatchGoldenHex() throws IOException {
    // Given
    var settings = new Settings(1, 50, false);

    // When
    var hex = HEX.formatHex(CODEC.encode(settings));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encode_WhenValuesDifferFromDefaults_ShouldMatchGoldenHex() throws IOException {
    // Given
    var settings = new Settings(2, 80, true);

    // When
    var hex = HEX.formatHex(CODEC.encode(settings));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decode_WhenValuesEqualDefaultsEncoded_ShouldReturnSettingsWithDefaults() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var settings = CODEC.decode(bytes);

    // Then
    assertThat(settings).isEqualTo(new Settings(1, 50, false));
  }

  @Test
  void decode_WhenValuesDifferFromDefaultsEncoded_ShouldReturnSettingsWithValues() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var settings = CODEC.decode(bytes);

    // Then
    assertThat(settings).isEqualTo(new Settings(2, 80, true));
  }

  @Test
  void construct_WhenVolumeIsNegative_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Settings(1, -1, false));

    // Then
    assertThat(thrown).hasMessageContaining("volume must be >= 0");
  }

  @Test
  void construct_WhenVolumeExceedsMax_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Settings(1, 150, false));

    // Then
    assertThat(thrown).hasMessageContaining("volume must be <= 100");
  }

  @Test
  void construct_WhenIdExceedsMax_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Settings(256, 50, false));

    // Then
    assertThat(thrown).hasMessageContaining("id must be <= 255");
  }
}
