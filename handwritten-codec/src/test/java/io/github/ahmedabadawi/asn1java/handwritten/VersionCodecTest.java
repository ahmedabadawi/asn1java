package io.github.ahmedabadawi.asn1java.handwritten;

import io.github.ahmedabadawi.asn1java.handwritten.simple.Version;
import io.github.ahmedabadawi.asn1java.handwritten.simple.VersionCodec;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class VersionCodecTest {

    private static final VersionCodec CODEC = new VersionCodec();
    private static final Path GOLDEN_DIR = Paths.get(
            System.getProperty("user.dir")).getParent().resolve("golden-tests/simple");

    private String goldenHex(String name) throws IOException {
        return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
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
    void encodeValid1() throws IOException {
        assertEquals(goldenHex("valid-1"), toHex(CODEC.encode(new Version(1, 0))));
    }

    @Test
    void encodeValid2() throws IOException {
        assertEquals(goldenHex("valid-2"), toHex(CODEC.encode(new Version(2, 24))));
    }

    @Test
    void decodeValid1() throws IOException {
        assertEquals(new Version(1, 0), CODEC.decode(fromHex(goldenHex("valid-1"))));
    }

    @Test
    void decodeValid2() throws IOException {
        assertEquals(new Version(2, 24), CODEC.decode(fromHex(goldenHex("valid-2"))));
    }

    @Test
    void encodeRejectNegative() {
        assertThrows(IllegalArgumentException.class, () -> CODEC.encode(new Version(-1, 0)));
    }
}
