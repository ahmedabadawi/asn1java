package io.github.ahmedabadawi.asn1java.handwritten;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ahmedabadawi.asn1java.handwritten.packet.Packet;
import io.github.ahmedabadawi.asn1java.handwritten.packet.PacketCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class PacketCodecTest {

  private static final PacketCodec CODEC = new PacketCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/packet");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encodeValid1() throws IOException {
    var payload = HEX.parseHex("deadbeef");
    assertThat(HEX.formatHex(CODEC.encode(new Packet(payload)))).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encodeValid2() throws IOException {
    var payload = HEX.parseHex("ff");
    assertThat(HEX.formatHex(CODEC.encode(new Packet(payload)))).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decodeValid1() throws IOException {
    var decoded = CODEC.decode(HEX.parseHex(goldenHex("valid-1")));
    assertThat(decoded.payload()).containsExactly(HEX.parseHex("deadbeef"));
  }

  @Test
  void decodeValid2() throws IOException {
    var decoded = CODEC.decode(HEX.parseHex(goldenHex("valid-2")));
    assertThat(decoded.payload()).containsExactly(HEX.parseHex("ff"));
  }

  @Test
  void encode_WhenPayloadExceedsSizeLimit_ShouldThrowIllegalArgumentException() {
    var payload = HEX.parseHex("0102030405");
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> CODEC.encode(new Packet(payload)));
    assertThat(thrown).hasMessageContaining("payload length must be in range 1..4");
  }

  @Test
  void encode_WhenPayloadIsEmpty_ShouldThrowIllegalArgumentException() {
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> CODEC.encode(new Packet(new byte[0])));
    assertThat(thrown).hasMessageContaining("payload length must be in range 1..4");
  }
}
