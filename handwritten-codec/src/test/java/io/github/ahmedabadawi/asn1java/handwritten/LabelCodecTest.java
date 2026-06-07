package io.github.ahmedabadawi.asn1java.handwritten;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ahmedabadawi.asn1java.handwritten.label.Label;
import io.github.ahmedabadawi.asn1java.handwritten.label.LabelCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class LabelCodecTest {

  private static final LabelCodec CODEC = new LabelCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/label");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encodeValid1() throws IOException {
    assertThat(HEX.formatHex(CODEC.encode(new Label("hi")))).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encodeValid2() throws IOException {
    assertThat(HEX.formatHex(CODEC.encode(new Label("hello")))).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decodeValid1() throws IOException {
    assertThat(CODEC.decode(HEX.parseHex(goldenHex("valid-1")))).isEqualTo(new Label("hi"));
  }

  @Test
  void decodeValid2() throws IOException {
    assertThat(CODEC.decode(HEX.parseHex(goldenHex("valid-2")))).isEqualTo(new Label("hello"));
  }

  @Test
  void encode_WhenTextIsEmpty_ShouldThrowIllegalArgumentException() {
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> CODEC.encode(new Label("")));
    assertThat(thrown).hasMessageContaining("text length must be >= 1");
  }
}
