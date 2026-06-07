package io.github.ahmedabadawi.asn1java.handwritten;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ahmedabadawi.asn1java.handwritten.identifier.Identifier;
import io.github.ahmedabadawi.asn1java.handwritten.identifier.IdentifierCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class IdentifierCodecTest {

  private static final IdentifierCodec CODEC = new IdentifierCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/identifier");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encodeValid1() throws IOException {
    assertThat(HEX.formatHex(CODEC.encode(new Identifier("ABC", "hi")))).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encodeValid2() throws IOException {
    assertThat(HEX.formatHex(CODEC.encode(new Identifier("XYZ", "world")))).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decodeValid1() throws IOException {
    var decoded = CODEC.decode(HEX.parseHex(goldenHex("valid-1")));
    assertThat(decoded).isEqualTo(new Identifier("ABC", "hi"));
  }

  @Test
  void decodeValid2() throws IOException {
    var decoded = CODEC.decode(HEX.parseHex(goldenHex("valid-2")));
    assertThat(decoded).isEqualTo(new Identifier("XYZ", "world"));
  }

  @Test
  void encode_WhenCodeIsEmpty_ShouldThrowIllegalArgumentException() {
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> CODEC.encode(new Identifier("", "hi")));
    assertThat(thrown).hasMessageContaining("code length must be in range 1..8");
  }
}
