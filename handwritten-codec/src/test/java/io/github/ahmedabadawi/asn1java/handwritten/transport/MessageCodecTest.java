package io.github.ahmedabadawi.asn1java.handwritten.transport;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class MessageCodecTest {

  private static final MessageCodec CODEC = new MessageCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR =
      Paths.get(System.getProperty("user.dir")).getParent().resolve("golden-tests/transport-message");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  private Message valid1() {
    return new Message(
        new ProtocolVersion(new VersionSingle(1), new VersionSingle(0)),
        new MessageType("hi"),
        new Payload(0, new byte[]{(byte) 0xff}));
  }

  private Message valid2() {
    return new Message(
        new ProtocolVersion(new VersionSingle(2), new VersionSingle(3)),
        new MessageType("hello"),
        new Payload(3600, HEX.parseHex("deadbeef")));
  }

  @Test
  void encodeValid1() throws IOException {
    // Given
    var message = valid1();

    // When
    var encoded = CODEC.encode(message);

    // Then
    assertThat(HEX.formatHex(encoded)).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encodeValid2() throws IOException {
    // Given
    var message = valid2();

    // When
    var encoded = CODEC.encode(message);

    // Then
    assertThat(HEX.formatHex(encoded)).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decodeValid1() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var decoded = CODEC.decode(bytes);

    // Then
    assertThat(decoded.protocolVersion().major().value()).isEqualTo(1);
    assertThat(decoded.protocolVersion().minor().value()).isEqualTo(0);
    assertThat(decoded.messageType().value()).isEqualTo("hi");
    assertThat(decoded.payload().messageTimeToLive()).isEqualTo(0);
    assertThat(decoded.payload().data()).containsExactly(0xff);
  }

  @Test
  void decodeValid2() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var decoded = CODEC.decode(bytes);

    // Then
    assertThat(decoded.protocolVersion().major().value()).isEqualTo(2);
    assertThat(decoded.protocolVersion().minor().value()).isEqualTo(3);
    assertThat(decoded.messageType().value()).isEqualTo("hello");
    assertThat(decoded.payload().messageTimeToLive()).isEqualTo(3600);
    assertThat(decoded.payload().data()).containsExactly(0xde, 0xad, 0xbe, 0xef);
  }

  @Test
  void encode_WhenMessageTypeTooShort_ShouldThrowIllegalArgumentException() {
    // Given
    var message = new Message(
        new ProtocolVersion(new VersionSingle(1), new VersionSingle(0)),
        new MessageType("x"),
        new Payload(0, new byte[]{(byte) 0xff}));

    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> CODEC.encode(message));

    // Then
    assertThat(thrown).hasMessageContaining("length must be >= 2");
  }
}
