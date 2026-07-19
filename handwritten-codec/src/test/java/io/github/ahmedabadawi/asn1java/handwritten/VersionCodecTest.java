package io.github.ahmedabadawi.asn1java.handwritten;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.ahmedabadawi.asn1java.handwritten.simple.Version;
import io.github.ahmedabadawi.asn1java.handwritten.simple.VersionCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class VersionCodecTest {

    private static final VersionCodec CODEC = new VersionCodec();
    private static final HexFormat HEX = HexFormat.of();
    private static final Path GOLDEN_DIR = Paths.get(
            System.getProperty("user.dir")).getParent().resolve("golden-tests/simple");

    private String goldenHex(String name) throws IOException {
        return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
    }

    @Test
    void encodeValid1() throws IOException {
        assertThat(HEX.formatHex(CODEC.encode(new Version(1, 0)))).isEqualTo(goldenHex("valid-1"));
    }

    @Test
    void encodeValid2() throws IOException {
        assertThat(HEX.formatHex(CODEC.encode(new Version(2, 24)))).isEqualTo(goldenHex("valid-2"));
    }

    @Test
    void decodeValid1() throws IOException {
        assertThat(CODEC.decode(HEX.parseHex(goldenHex("valid-1")))).isEqualTo(new Version(1, 0));
    }

    @Test
    void decodeValid2() throws IOException {
        assertThat(CODEC.decode(HEX.parseHex(goldenHex("valid-2")))).isEqualTo(new Version(2, 24));
    }

    @Test
    void construct_WhenMajorIsNegative_ShouldThrowIllegalArgumentException() {
        // When
        var thrown = catchThrowableOfType(IllegalArgumentException.class,
                () -> new Version(-1, 0));

        // Then
        assertThat(thrown).hasMessageContaining("major and minor must be >= 0");
    }
}
