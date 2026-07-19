package io.github.ahmedabadawi.asn1java.handwritten.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ContactCodecTest {

  private static final ContactCodec CODEC = new ContactCodec();
  private static final HexFormat HEX = HexFormat.of();
  private static final Path GOLDEN_DIR = Paths.get(
      System.getProperty("user.dir")).getParent().resolve("golden-tests/contact");

  private String goldenHex(String name) throws IOException {
    return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
  }

  @Test
  void encode_WhenAgePresent_ShouldMatchGoldenHex() throws IOException {
    // Given
    var contact = new Contact(1, 30);

    // When
    var hex = HEX.formatHex(CODEC.encode(contact));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-1"));
  }

  @Test
  void encode_WhenAgeAbsent_ShouldMatchGoldenHex() throws IOException {
    // Given
    var contact = new Contact(2, null);

    // When
    var hex = HEX.formatHex(CODEC.encode(contact));

    // Then
    assertThat(hex).isEqualTo(goldenHex("valid-2"));
  }

  @Test
  void decode_WhenAgePresentEncoded_ShouldReturnContactWithAge() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-1"));

    // When
    var contact = CODEC.decode(bytes);

    // Then
    assertThat(contact).isEqualTo(new Contact(1, 30));
  }

  @Test
  void decode_WhenAgeAbsentEncoded_ShouldReturnContactWithNullAge() throws IOException {
    // Given
    var bytes = HEX.parseHex(goldenHex("valid-2"));

    // When
    var contact = CODEC.decode(bytes);

    // Then
    assertThat(contact).isEqualTo(new Contact(2, null));
  }

  @Test
  void construct_WhenAgeIsNull_ShouldNotThrow() {
    // When
    var contact = new Contact(1, null);

    // Then
    assertThat(contact.age()).isNull();
  }

  @Test
  void construct_WhenIdIsNegative_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Contact(-1, 30));

    // Then
    assertThat(thrown).hasMessageContaining("id must be >= 0");
  }

  @Test
  void construct_WhenIdExceedsMax_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Contact(256, 30));

    // Then
    assertThat(thrown).hasMessageContaining("id must be <= 255");
  }

  @Test
  void construct_WhenAgeExceedsMax_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Contact(3, 999));

    // Then
    assertThat(thrown).hasMessageContaining("age must be <= 255");
  }

  @Test
  void construct_WhenAgeIsNegative_ShouldThrowIllegalArgumentException() {
    // When
    var thrown = catchThrowableOfType(IllegalArgumentException.class,
        () -> new Contact(3, -1));

    // Then
    assertThat(thrown).hasMessageContaining("age must be >= 0");
  }
}
