package io.github.ahmedabadawi.asn1java.handwritten;

import io.github.ahmedabadawi.asn1java.handwritten.device.Device;
import io.github.ahmedabadawi.asn1java.handwritten.device.DeviceCodec;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceCodecTest {

    private static final DeviceCodec CODEC = new DeviceCodec();
    private static final Path GOLDEN_DIR = Paths.get(
            System.getProperty("user.dir")).getParent().resolve("golden-tests/device");

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
    void encode_WhenActiveIsTrue_ShouldMatchGoldenHex() throws IOException {
        // Given
        var device = new Device(true);

        // When
        var hex = toHex(CODEC.encode(device));

        // Then
        assertThat(hex).isEqualTo(goldenHex("valid-1"));
    }

    @Test
    void encode_WhenActiveIsFalse_ShouldMatchGoldenHex() throws IOException {
        // Given
        var device = new Device(false);

        // When
        var hex = toHex(CODEC.encode(device));

        // Then
        assertThat(hex).isEqualTo(goldenHex("valid-2"));
    }

    @Test
    void decode_WhenTrueBitEncoded_ShouldReturnActiveTrue() throws IOException {
        // Given
        var bytes = fromHex(goldenHex("valid-1"));

        // When
        var device = CODEC.decode(bytes);

        // Then
        assertThat(device).isEqualTo(new Device(true));
    }

    @Test
    void decode_WhenFalseBitEncoded_ShouldReturnActiveFalse() throws IOException {
        // Given
        var bytes = fromHex(goldenHex("valid-2"));

        // When
        var device = CODEC.decode(bytes);

        // Then
        assertThat(device).isEqualTo(new Device(false));
    }
}
