package io.github.ahmedabadawi.asn1java.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ahmedabadawi.asn1java.sample.markermodule.Marker;
import io.github.ahmedabadawi.asn1java.sample.markermodule.MarkerCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class MarkerCodecApprovalTest {

  private static final MarkerCodec CODEC = new MarkerCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/marker");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encode_WhenMarker_ShouldProduceEmptyBytes() throws IOException {
    // Given
    var marker = new Marker();

    // When
    var hex = HEX.formatHex(CODEC.encode(marker));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void decode_WhenEmptyBytes_ShouldReturnMarker() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var marker = CODEC.decode(bytes);

    // Then
    assertThat(marker).isEqualTo(new Marker());
  }
}
