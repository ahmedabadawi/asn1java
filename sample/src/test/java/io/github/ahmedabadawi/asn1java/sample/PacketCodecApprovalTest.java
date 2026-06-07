package io.github.ahmedabadawi.asn1java.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ahmedabadawi.asn1java.sample.packetmodule.Packet;
import io.github.ahmedabadawi.asn1java.sample.packetmodule.PacketCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class PacketCodecApprovalTest {

  private static final PacketCodec CODEC = new PacketCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/packet");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encode_WhenPayloadIs4Bytes_ShouldMatchGoldenHex() throws IOException {
    // Given
    var packet = new Packet(HEX.parseHex("deadbeef"));

    // When
    var hex = HEX.formatHex(CODEC.encode(packet));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encode_WhenPayloadIs1Byte_ShouldMatchGoldenHex() throws IOException {
    // Given
    var packet = new Packet(HEX.parseHex("ff"));

    // When
    var hex = HEX.formatHex(CODEC.encode(packet));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decode_WhenValid1Encoded_ShouldReturn4BytePayload() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var packet = CODEC.decode(bytes);

    // Then
    assertThat(packet.payload()).containsExactly(HEX.parseHex("deadbeef"));
  }

  @Test
  void decode_WhenValid2Encoded_ShouldReturn1BytePayload() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var packet = CODEC.decode(bytes);

    // Then
    assertThat(packet.payload()).containsExactly(HEX.parseHex("ff"));
  }
}
