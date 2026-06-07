package io.github.ahmedabadawi.asn1java.handwritten;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ahmedabadawi.asn1java.handwritten.flags.Flags;
import io.github.ahmedabadawi.asn1java.handwritten.flags.FlagsCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class FlagsCodecTest {

  private static final FlagsCodec CODEC = new FlagsCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/flags");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encodeValid1() throws IOException {
    assertThat(HEX.formatHex(CODEC.encode(new Flags(HEX.parseHex("b2"))))).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encodeValid2() throws IOException {
    assertThat(HEX.formatHex(CODEC.encode(new Flags(HEX.parseHex("ff"))))).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decodeValid1() throws IOException {
    var decoded = CODEC.decode(HEX.parseHex(goldenHex("valid-1")));
    assertThat(decoded.bits()).containsExactly(HEX.parseHex("b2"));
  }

  @Test
  void decodeValid2() throws IOException {
    var decoded = CODEC.decode(HEX.parseHex(goldenHex("valid-2")));
    assertThat(decoded.bits()).containsExactly(HEX.parseHex("ff"));
  }

  @Test
  void encode_WhenBitsWrongSize_ShouldThrowIllegalArgumentException() {
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> CODEC.encode(new Flags(HEX.parseHex("b2b3"))));
    assertThat(thrown).hasMessageContaining("bits must be exactly 8 bits");
  }
}
