package io.github.ahmedabadawi.asn1java.runtime.uper;

public final class UperCodecSupport {

    private UperCodecSupport() {}

    // X.691 §12.2.6: semi-constrained whole number
    // Encoded as: 1-byte length determinant + value bytes (big-endian, minimum 1 octet)
    public static void encodeSemiConstrainedInt(UperOutputStream out, long value) {
        int octets = Math.max(1, (Long.SIZE - Long.numberOfLeadingZeros(value) + 7) / 8);
        out.writeBits(octets, 8);
        out.writeBits(value, octets * 8);
    }

    public static long decodeSemiConstrainedInt(UperInputStream in) {
        int octets = (int) in.readBits(8);
        return in.readBits(octets * 8);
    }
}
