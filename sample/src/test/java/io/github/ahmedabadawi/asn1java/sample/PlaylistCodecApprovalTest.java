package io.github.ahmedabadawi.asn1java.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ahmedabadawi.asn1java.sample.playlistmodule.Playlist;
import io.github.ahmedabadawi.asn1java.sample.playlistmodule.PlaylistCodec;
import io.github.ahmedabadawi.asn1java.sample.playlistmodule.Track;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaylistCodecApprovalTest {

  private static final PlaylistCodec CODEC = new PlaylistCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/playlist");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  private static Playlist valid1() {
    return new Playlist(
        List.of("rock", "pop"),
        List.of(new Track("Song A")),
        List.of(new Track("T1"), new Track("T2"), new Track("T3")));
  }

  private static Playlist valid2() {
    return new Playlist(
        List.of(),
        List.of(new Track("Alpha"), new Track("Beta"), new Track("Gamma")),
        List.of(new Track("X"), new Track("Y"), new Track("Z")));
  }

  @Test
  void encode_WhenValid1_ShouldMatchGoldenHex() throws IOException {
    // Given
    var playlist = valid1();

    // When
    var hex = HEX.formatHex(CODEC.encode(playlist));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encode_WhenValid2_ShouldMatchGoldenHex() throws IOException {
    // Given
    var playlist = valid2();

    // When
    var hex = HEX.formatHex(CODEC.encode(playlist));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decode_WhenValid1Encoded_ShouldReturnPlaylist() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var playlist = CODEC.decode(bytes);

    // Then
    assertThat(playlist).isEqualTo(valid1());
  }

  @Test
  void decode_WhenValid2Encoded_ShouldReturnPlaylist() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var playlist = CODEC.decode(bytes);

    // Then
    assertThat(playlist).isEqualTo(valid2());
  }

  @Test
  void construct_WhenTracksIsEmpty_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Playlist(List.of(), List.of(),
            List.of(new Track("T1"), new Track("T2"), new Track("T3"))));

    // Then
    assertThat(thrown).hasMessageContaining("tracks must have between 1 and 64 elements");
  }
}
