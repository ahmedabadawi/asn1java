package io.github.ahmedabadawi.asn1java.runtime.uper;

import java.nio.charset.StandardCharsets;

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

  // X.691 §26 + §10.7: unconstrained UTF8String
  // Length determinant: 1 byte if < 128 octets, 2 bytes (0x80 | hi, lo) otherwise; then UTF-8 bytes
  public static void encodeUtf8String(UperOutputStream out, String value) {
    var utf8 = value.getBytes(StandardCharsets.UTF_8);
    int len = utf8.length;
    if (len < 128) {
      out.writeBits(len, 8);
    } else {
      out.writeBits(0x80 | (len >> 8), 8);
      out.writeBits(len & 0xFF, 8);
    }
    for (byte b : utf8) {
      out.writeBits(b & 0xFF, 8);
    }
  }

  public static String decodeUtf8String(UperInputStream in) {
    int first = (int) in.readBits(8);
    int len = (first & 0x80) == 0 ? first : ((first & 0x3F) << 8) | (int) in.readBits(8);
    var utf8 = new byte[len];
    for (int i = 0; i < len; i++) {
      utf8[i] = (byte) in.readBits(8);
    }
    return new String(utf8, StandardCharsets.UTF_8);
  }
}
