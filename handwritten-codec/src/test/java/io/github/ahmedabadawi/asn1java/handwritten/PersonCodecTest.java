package io.github.ahmedabadawi.asn1java.handwritten;

import io.github.ahmedabadawi.asn1java.handwritten.person.Person;
import io.github.ahmedabadawi.asn1java.handwritten.person.PersonCodec;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class PersonCodecTest {

    private static final PersonCodec CODEC = new PersonCodec();
    private static final Path GOLDEN_DIR = Paths.get(
            System.getProperty("user.dir")).getParent().resolve("golden-tests/person");

    private String goldenHex(String name) throws IOException {
        return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
    }

    private static String toHex(byte[] bytes) {
        var sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append("%02x".formatted(b));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex, i * 2, i * 2 + 2, 16);
        }
        return bytes;
    }

    @Test
    void encode_WhenNameIsHello_ShouldMatchGoldenHex() throws IOException {
        // Given
        var person = new Person("hello");

        // When
        var hex = toHex(CODEC.encode(person));

        // Then
        assertThat(hex).isEqualTo(goldenHex("valid-1"));
    }

    @Test
    void encode_WhenNameIsEmpty_ShouldMatchGoldenHex() throws IOException {
        // Given
        var person = new Person("");

        // When
        var hex = toHex(CODEC.encode(person));

        // Then
        assertThat(hex).isEqualTo(goldenHex("valid-2"));
    }

    @Test
    void decode_WhenHelloEncoded_ShouldReturnNameHello() throws IOException {
        // Given
        var bytes = fromHex(goldenHex("valid-1"));

        // When
        var person = CODEC.decode(bytes);

        // Then
        assertThat(person).isEqualTo(new Person("hello"));
    }

    @Test
    void decode_WhenEmptyEncoded_ShouldReturnEmptyName() throws IOException {
        // Given
        var bytes = fromHex(goldenHex("valid-2"));

        // When
        var person = CODEC.decode(bytes);

        // Then
        assertThat(person).isEqualTo(new Person(""));
    }

    @Test
    void encode_WhenNameIsNull_ShouldThrowIllegalArgumentException() {
        // Given
        var person = new Person(null);

        // When
        var thrown = catchThrowableOfType(IllegalArgumentException.class,
                () -> CODEC.encode(person));

        // Then
        assertThat(thrown).hasMessageContaining("name must not be null");
    }
}
