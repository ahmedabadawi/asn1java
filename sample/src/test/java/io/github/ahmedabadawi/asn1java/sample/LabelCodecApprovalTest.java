package io.github.ahmedabadawi.asn1java.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ahmedabadawi.asn1java.sample.labelmodule.Label;
import io.github.ahmedabadawi.asn1java.sample.labelmodule.LabelCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class LabelCodecApprovalTest {

  private static final LabelCodec CODEC = new LabelCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/label");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encode_WhenTextIsHi_ShouldMatchGoldenHex() throws IOException {
    // Given
    var label = new Label("hi");

    // When
    var hex = HEX.formatHex(CODEC.encode(label));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encode_WhenTextIsHello_ShouldMatchGoldenHex() throws IOException {
    // Given
    var label = new Label("hello");

    // When
    var hex = HEX.formatHex(CODEC.encode(label));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decode_WhenValid1Encoded_ShouldReturnHi() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var label = CODEC.decode(bytes);

    // Then
    assertThat(label).isEqualTo(new Label("hi"));
  }

  @Test
  void decode_WhenValid2Encoded_ShouldReturnHello() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var label = CODEC.decode(bytes);

    // Then
    assertThat(label).isEqualTo(new Label("hello"));
  }
}
