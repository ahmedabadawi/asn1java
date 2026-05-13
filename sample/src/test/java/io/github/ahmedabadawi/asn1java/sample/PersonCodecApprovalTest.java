package io.github.ahmedabadawi.asn1java.sample;

import static io.github.ahmedabadawi.asn1java.sample.TestHelpers.fromHex;
import static io.github.ahmedabadawi.asn1java.sample.TestHelpers.toHex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ahmedabadawi.asn1java.sample.personinfo.Person;
import io.github.ahmedabadawi.asn1java.sample.personinfo.PersonCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class PersonCodecApprovalTest {

    private static final PersonCodec CODEC = new PersonCodec();
    private static final Path GOLDEN_DIR = Paths.get(
            System.getProperty("user.dir")).getParent().resolve("golden-tests/person");

    private String goldenHex(String name) throws IOException {
        return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
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
