package io.github.ahmedabadawi.asn1java.handwritten.simple;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class VersionCodec {

    public byte[] encode(Version version) {
        UperOutputStream out = new UperOutputStream();
        encodeSemiConstrainedInt(out, version.major());
        encodeSemiConstrainedInt(out, version.minor());
        return out.toByteArray();
    }

    public Version decode(byte[] data) {
        UperInputStream in = new UperInputStream(data);
        int major = (int) decodeSemiConstrainedInt(in);
        int minor = (int) decodeSemiConstrainedInt(in);
        return new Version(major, minor);
    }

    // X.691 §12.2.6: semi-constrained whole number (lb=0, no upper bound)
    // Encoded as: 1-byte length determinant + value bytes (big-endian, minimum 1 octet)
    private void encodeSemiConstrainedInt(UperOutputStream out, long value) {
        int octets = Math.max(1, (Long.SIZE - Long.numberOfLeadingZeros(value) + 7) / 8);
        out.writeBits(octets, 8);
        out.writeBits(value, octets * 8);
    }

    private long decodeSemiConstrainedInt(UperInputStream in) {
        int octets = (int) in.readBits(8);
        return in.readBits(octets * 8);
    }
}
