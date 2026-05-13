package io.github.ahmedabadawi.asn1java.handwritten;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ahmedabadawi.asn1java.handwritten.device.Device;
import io.github.ahmedabadawi.asn1java.handwritten.device.DeviceCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class DeviceCodecTest {

    private static final DeviceCodec CODEC = new DeviceCodec();
    private static final HexFormat HEX = HexFormat.of();
    private static final Path GOLDEN_DIR = Paths.get(
            System.getProperty("user.dir")).getParent().resolve("golden-tests/device");

    private String goldenHex(String name) throws IOException {
        return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
    }

    @Test
    void encode_WhenActiveIsTrue_ShouldMatchGoldenHex() throws IOException {
        // Given
        var device = new Device(true);

        // When
        var hex = HEX.formatHex(CODEC.encode(device));

        // Then
        assertThat(hex).isEqualTo(goldenHex("valid-1"));
    }

    @Test
    void encode_WhenActiveIsFalse_ShouldMatchGoldenHex() throws IOException {
        // Given
        var device = new Device(false);

        // When
        var hex = HEX.formatHex(CODEC.encode(device));

        // Then
        assertThat(hex).isEqualTo(goldenHex("valid-2"));
    }

    @Test
    void decode_WhenTrueBitEncoded_ShouldReturnActiveTrue() throws IOException {
        // Given
        var bytes = HEX.parseHex(goldenHex("valid-1"));

        // When
        var device = CODEC.decode(bytes);

        // Then
        assertThat(device).isEqualTo(new Device(true));
    }

    @Test
    void decode_WhenFalseBitEncoded_ShouldReturnActiveFalse() throws IOException {
        // Given
        var bytes = HEX.parseHex(goldenHex("valid-2"));

        // When
        var device = CODEC.decode(bytes);

        // Then
        assertThat(device).isEqualTo(new Device(false));
    }
}
