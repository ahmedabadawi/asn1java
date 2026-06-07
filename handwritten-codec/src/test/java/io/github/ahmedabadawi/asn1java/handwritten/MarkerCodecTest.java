package io.github.ahmedabadawi.asn1java.handwritten;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ahmedabadawi.asn1java.handwritten.marker.Marker;
import io.github.ahmedabadawi.asn1java.handwritten.marker.MarkerCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class MarkerCodecTest {

  private static final MarkerCodec CODEC = new MarkerCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/marker");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encode_WhenMarker_ShouldProduceEmptyBytes() throws IOException {
    assertThat(HEX.formatHex(CODEC.encode(new Marker()))).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void decode_WhenEmptyBytes_ShouldReturnMarker() throws IOException {
    var decoded = CODEC.decode(HEX.parseHex(goldenHex("valid-1")));
    assertThat(decoded).isEqualTo(new Marker());
  }
}
