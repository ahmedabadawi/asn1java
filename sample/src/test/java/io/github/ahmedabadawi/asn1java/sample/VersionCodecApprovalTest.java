package io.github.ahmedabadawi.asn1java.sample;

import static io.github.ahmedabadawi.asn1java.sample.TestHelpers.fromHex;
import static io.github.ahmedabadawi.asn1java.sample.TestHelpers.toHex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.ahmedabadawi.asn1java.sample.versioninfo.Version;
import io.github.ahmedabadawi.asn1java.sample.versioninfo.VersionCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class VersionCodecApprovalTest {

    private static final VersionCodec CODEC = new VersionCodec();
    private static final Path GOLDEN_DIR = Paths.get(
            System.getProperty("user.dir")).getParent().resolve("golden-tests/simple");

    private String goldenHex(String name) throws IOException {
        return Files.readString(GOLDEN_DIR.resolve(name + ".hex")).strip();
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
