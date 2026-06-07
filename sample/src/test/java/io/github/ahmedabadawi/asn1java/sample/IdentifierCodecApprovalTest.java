package io.github.ahmedabadawi.asn1java.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ahmedabadawi.asn1java.sample.identifiermodule.Identifier;
import io.github.ahmedabadawi.asn1java.sample.identifiermodule.IdentifierCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class IdentifierCodecApprovalTest {

  private static final IdentifierCodec CODEC = new IdentifierCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/identifier");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encode_WhenCodeIsABCAndLabelIsHi_ShouldMatchGoldenHex() throws IOException {
    // Given
    var identifier = new Identifier("ABC", "hi");

    // When
    var hex = HEX.formatHex(CODEC.encode(identifier));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encode_WhenCodeIsXYZAndLabelIsWorld_ShouldMatchGoldenHex() throws IOException {
    // Given
    var identifier = new Identifier("XYZ", "world");

    // When
    var hex = HEX.formatHex(CODEC.encode(identifier));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decode_WhenValid1Encoded_ShouldReturnABCHi() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var identifier = CODEC.decode(bytes);

    // Then
    assertThat(identifier).isEqualTo(new Identifier("ABC", "hi"));
  }

  @Test
  void decode_WhenValid2Encoded_ShouldReturnXYZWorld() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var identifier = CODEC.decode(bytes);

    // Then
    assertThat(identifier).isEqualTo(new Identifier("XYZ", "world"));
  }
}
