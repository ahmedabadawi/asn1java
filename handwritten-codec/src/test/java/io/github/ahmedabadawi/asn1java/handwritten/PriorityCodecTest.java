package io.github.ahmedabadawi.asn1java.handwritten;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ahmedabadawi.asn1java.handwritten.priority.Priority;
import io.github.ahmedabadawi.asn1java.handwritten.priority.PriorityCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class PriorityCodecTest {

  private static final PriorityCodec CODEC = new PriorityCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/priority");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encodeValid1() throws IOException {
    assertThat(HEX.formatHex(CODEC.encode(new Priority(0, 0)))).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encodeValid2() throws IOException {
    assertThat(HEX.formatHex(CODEC.encode(new Priority(2, -100)))).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decodeValid1() throws IOException {
    var decoded = CODEC.decode(HEX.parseHex(goldenHex("valid-1")));
    assertThat(decoded).isEqualTo(new Priority(0, 0));
  }

  @Test
  void decodeValid2() throws IOException {
    var decoded = CODEC.decode(HEX.parseHex(goldenHex("valid-2")));
    assertThat(decoded).isEqualTo(new Priority(2, -100));
  }

  @Test
  void construct_WhenLevelExceedsMax_ShouldThrowIllegalArgumentException() {
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Priority(3, 0));
    assertThat(thrown).hasMessageContaining("level must be in range 0..2");
  }

  @Test
  void construct_WhenAdjustmentExceedsUpperBound_ShouldThrowIllegalArgumentException() {
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Priority(0, 1));
    assertThat(thrown).hasMessageContaining("adjustment must be <= 0");
  }
}
