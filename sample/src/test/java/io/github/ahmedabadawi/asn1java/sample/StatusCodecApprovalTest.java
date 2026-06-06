package io.github.ahmedabadawi.asn1java.sample;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ahmedabadawi.asn1java.sample.statusmodule.Status;
import io.github.ahmedabadawi.asn1java.sample.statusmodule.StatusCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class StatusCodecApprovalTest {

  private static final StatusCodec CODEC = new StatusCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/status");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encode_WhenStateIsPending_ShouldMatchGoldenHex() throws IOException {
    // Given
    var status = new Status(0);

    // When
    var hex = HEX.formatHex(CODEC.encode(status));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encode_WhenStateIsActive_ShouldMatchGoldenHex() throws IOException {
    // Given
    var status = new Status(1);

    // When
    var hex = HEX.formatHex(CODEC.encode(status));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void encode_WhenStateIsInactive_ShouldMatchGoldenHex() throws IOException {
    // Given
    var status = new Status(2);

    // When
    var hex = HEX.formatHex(CODEC.encode(status));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-3"));
  }

  @Test
  void decode_WhenPendingEncoded_ShouldReturnState0() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var status = CODEC.decode(bytes);

    // Then
    assertThat(status).isEqualTo(new Status(0));
  }

  @Test
  void decode_WhenActiveEncoded_ShouldReturnState1() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var status = CODEC.decode(bytes);

    // Then
    assertThat(status).isEqualTo(new Status(1));
  }

  @Test
  void decode_WhenInactiveEncoded_ShouldReturnState2() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-3"));

    // When
    var status = CODEC.decode(bytes);

    // Then
    assertThat(status).isEqualTo(new Status(2));
  }
}
