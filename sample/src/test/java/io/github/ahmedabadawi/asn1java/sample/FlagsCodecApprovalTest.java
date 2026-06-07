package io.github.ahmedabadawi.asn1java.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ahmedabadawi.asn1java.sample.flagsmodule.Flags;
import io.github.ahmedabadawi.asn1java.sample.flagsmodule.FlagsCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class FlagsCodecApprovalTest {

  private static final FlagsCodec CODEC = new FlagsCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/flags");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encode_WhenBitsIsB2_ShouldMatchGoldenHex() throws IOException {
    // Given
    var flags = new Flags(HEX.parseHex("b2"));

    // When
    var hex = HEX.formatHex(CODEC.encode(flags));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encode_WhenBitsIsFF_ShouldMatchGoldenHex() throws IOException {
    // Given
    var flags = new Flags(HEX.parseHex("ff"));

    // When
    var hex = HEX.formatHex(CODEC.encode(flags));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decode_WhenValid1Encoded_ShouldReturnB2Bits() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var flags = CODEC.decode(bytes);

    // Then
    assertThat(flags.bits()).containsExactly(HEX.parseHex("b2"));
  }

  @Test
  void decode_WhenValid2Encoded_ShouldReturnFFBits() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var flags = CODEC.decode(bytes);

    // Then
    assertThat(flags.bits()).containsExactly(HEX.parseHex("ff"));
  }
}
