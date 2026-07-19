package io.github.ahmedabadawi.asn1java.handwritten.limits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class SettingsCodecTest {

  private static final SettingsCodec CODEC = new SettingsCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/limits");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encode_WhenValid1_ShouldMatchGoldenHex() throws IOException {
    // Given
    var settings = new Settings(50, List.of("loud", "clear"));

    // When
    var hex = HEX.formatHex(CODEC.encode(settings));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encode_WhenValid2_ShouldMatchGoldenHex() throws IOException {
    // Given
    var settings = new Settings(100, List.of());

    // When
    var hex = HEX.formatHex(CODEC.encode(settings));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decode_WhenValid1Encoded_ShouldReturnSettings() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var settings = CODEC.decode(bytes);

    // Then
    assertThat(settings).isEqualTo(new Settings(50, List.of("loud", "clear")));
  }

  @Test
  void decode_WhenValid2Encoded_ShouldReturnSettings() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var settings = CODEC.decode(bytes);

    // Then
    assertThat(settings).isEqualTo(new Settings(100, List.of()));
  }

  @Test
  void construct_WhenVolumeExceedsMax_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Settings(150, List.of()));

    // Then
    assertThat(thrown).hasMessageContaining("volume must be <= 100");
  }
}
