package io.github.ahmedabadawi.asn1java.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ahmedabadawi.asn1java.sample.scoreinfo.Score;
import io.github.ahmedabadawi.asn1java.sample.scoreinfo.ScoreCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ScoreCodecApprovalTest {

  private static final ScoreCodec CODEC = new ScoreCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/score");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encode_WhenMinValues_ShouldMatchGoldenHex() throws IOException {
    // Given
    var score = new Score(1, 0, -10);

    // When
    var hex = HEX.formatHex(CODEC.encode(score));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encode_WhenMidValues_ShouldMatchGoldenHex() throws IOException {
    // Given
    var score = new Score(5, 500, 0);

    // When
    var hex = HEX.formatHex(CODEC.encode(score));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void encode_WhenMaxValues_ShouldMatchGoldenHex() throws IOException {
    // Given
    var score = new Score(10, 999, 10);

    // When
    var hex = HEX.formatHex(CODEC.encode(score));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-3"));
  }

  @Test
  void decode_WhenMinValuesEncoded_ShouldReturnMinScore() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var score = CODEC.decode(bytes);

    // Then
    assertThat(score).isEqualTo(new Score(1, 0, -10));
  }

  @Test
  void decode_WhenMidValuesEncoded_ShouldReturnMidScore() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var score = CODEC.decode(bytes);

    // Then
    assertThat(score).isEqualTo(new Score(5, 500, 0));
  }

  @Test
  void decode_WhenMaxValuesEncoded_ShouldReturnMaxScore() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-3"));

    // When
    var score = CODEC.decode(bytes);

    // Then
    assertThat(score).isEqualTo(new Score(10, 999, 10));
  }

  @Test
  void encode_WhenLevelIsBelowMin_ShouldThrowIllegalArgumentException() {
    // Given
    var score = new Score(0, 0, 0);

    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> CODEC.encode(score));

    // Then
    assertThat(thrown).hasMessageContaining("level must be >= 1");
  }

  @Test
  void encode_WhenPointsIsNegative_ShouldThrowIllegalArgumentException() {
    // Given
    var score = new Score(1, -1, 0);

    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> CODEC.encode(score));

    // Then
    assertThat(thrown).hasMessageContaining("points must be >= 0");
  }

  @Test
  void encode_WhenOffsetIsBelowMin_ShouldThrowIllegalArgumentException() {
    // Given
    var score = new Score(1, 0, -11);

    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> CODEC.encode(score));

    // Then
    assertThat(thrown).hasMessageContaining("offset must be >= -10");
  }
}
