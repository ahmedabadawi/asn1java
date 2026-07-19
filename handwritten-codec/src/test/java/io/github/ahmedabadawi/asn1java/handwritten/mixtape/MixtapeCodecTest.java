package io.github.ahmedabadawi.asn1java.handwritten.mixtape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ahmedabadawi.asn1java.handwritten.playlist.Track;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class MixtapeCodecTest {

  private static final MixtapeCodec CODEC = new MixtapeCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/mixtape");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  private static Mixtape valid1() {
    return new Mixtape("DJ Sample", List.of(new Track("Song A"), new Track("Song B")));
  }

  private static Mixtape valid2() {
    return new Mixtape("Anon", List.of(new Track("Solo")));
  }

  @Test
  void encode_WhenValid1_ShouldMatchGoldenHex() throws IOException {
    // Given
    var mixtape = valid1();

    // When
    var hex = HEX.formatHex(CODEC.encode(mixtape));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encode_WhenValid2_ShouldMatchGoldenHex() throws IOException {
    // Given
    var mixtape = valid2();

    // When
    var hex = HEX.formatHex(CODEC.encode(mixtape));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decode_WhenValid1Encoded_ShouldReturnMixtape() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var mixtape = CODEC.decode(bytes);

    // Then
    assertThat(mixtape).isEqualTo(valid1());
  }

  @Test
  void decode_WhenValid2Encoded_ShouldReturnMixtape() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var mixtape = CODEC.decode(bytes);

    // Then
    assertThat(mixtape).isEqualTo(valid2());
  }

  @Test
  void construct_WhenTracksIsEmpty_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Mixtape("Nobody", List.of()));

    // Then
    assertThat(thrown).hasMessageContaining("tracks must have between 1 and 10 elements");
  }
}
