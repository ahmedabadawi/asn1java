package io.github.ahmedabadawi.asn1java.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ahmedabadawi.asn1java.sample.prioritymodule.Priority;
import io.github.ahmedabadawi.asn1java.sample.prioritymodule.PriorityCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class PriorityCodecApprovalTest {

  private static final PriorityCodec CODEC = new PriorityCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/priority");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encode_WhenLevelLowAndAdjustmentZero_ShouldMatchGoldenHex() throws IOException {
    // Given
    var priority = new Priority(0, 0L);

    // When
    var hex = HEX.formatHex(CODEC.encode(priority));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encode_WhenLevelHighAndAdjustmentNegative_ShouldMatchGoldenHex() throws IOException {
    // Given
    var priority = new Priority(2, -100L);

    // When
    var hex = HEX.formatHex(CODEC.encode(priority));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decode_WhenValid1Encoded_ShouldReturnLevelZeroAdjustmentZero() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var priority = CODEC.decode(bytes);

    // Then
    assertThat(priority.level()).isEqualTo(0);
    assertThat(priority.adjustment()).isEqualTo(0L);
  }

  @Test
  void decode_WhenValid2Encoded_ShouldReturnLevelTwoAdjustmentMinus100() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var priority = CODEC.decode(bytes);

    // Then
    assertThat(priority.level()).isEqualTo(2);
    assertThat(priority.adjustment()).isEqualTo(-100L);
  }
}
