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

  // X.691 §12.2.3: unconstrained (or upper-bounded) whole number
  // Encoded as: 1-byte octet count + value bytes (big-endian, minimum 1 octet, two's complement)
  public static void encodeUnconstrainedInt(UperOutputStream out, long value) {
    int octets = unconstrainedOctetCount(value);
    out.writeBits(octets, 8);
    out.writeBits(value, octets * 8);
  }

  public static long decodeUnconstrainedInt(UperInputStream in) {
    int octets = (int) in.readBits(8);
    long raw = in.readBits(octets * 8);
    int signBit = octets * 8 - 1;
    if (((raw >> signBit) & 1) == 1) {
      long mask = -1L << (octets * 8);
      return raw | mask;
    }
    return raw;
  }

  private static int unconstrainedOctetCount(long value) {
    if (value >= 0) {
      return Math.max(1, (Long.SIZE - Long.numberOfLeadingZeros(value) + 8) / 8);
    }
    long complement = ~value;
    return Math.max(1, (Long.SIZE - Long.numberOfLeadingZeros(complement) + 8) / 8);
  }

  // X.691 §16: OCTET STRING, SIZE-constrained (range lb..ub)
  // When ub >= 64K the standard (§16.7) requires the §10.7 unconstrained length determinant
  // (actual length in 1 or 2 bytes); otherwise the length offset is encoded as a constrained
  // whole number using bitCount bits.
  public static void encodeOctetString(UperOutputStream out, byte[] value, int lb, int ub) {
    if (ub >= 65536) {
      int len = value.length;
      if (len < 128) {
        out.writeBits(len, 8);
      } else {
        out.writeBits(0x80 | (len >> 8), 8);
        out.writeBits(len & 0xFF, 8);
      }
    } else {
      int range = ub - lb;
      int bitCount = Integer.SIZE - Integer.numberOfLeadingZeros(range);
      out.writeBits(value.length - lb, bitCount);
    }
    for (byte b : value) {
      out.writeBits(b & 0xFF, 8);
    }
  }

  public static byte[] decodeOctetString(UperInputStream in, int lb, int ub) {
    int length;
    if (ub >= 65536) {
      int first = (int) in.readBits(8);
      length = (first & 0x80) == 0 ? first : ((first & 0x3F) << 8) | (int) in.readBits(8);
    } else {
      int range = ub - lb;
      int bitCount = Integer.SIZE - Integer.numberOfLeadingZeros(range);
      length = (int) in.readBits(bitCount) + lb;
    }
    var result = new byte[length];
    for (int i = 0; i < length; i++) {
      result[i] = (byte) in.readBits(8);
    }
    return result;
  }

  // X.691 §16: OCTET STRING, fixed SIZE (n..n)
  // No length determinant; just n bytes.
  public static void encodeFixedOctetString(UperOutputStream out, byte[] value) {
    for (byte b : value) {
      out.writeBits(b & 0xFF, 8);
    }
  }

  public static byte[] decodeFixedOctetString(UperInputStream in, int size) {
    var result = new byte[size];
    for (int i = 0; i < size; i++) {
      result[i] = (byte) in.readBits(8);
    }
    return result;
  }

  // X.691 §27: IA5String and VisibleString — SIZE-constrained
  // Constrained length (in characters) encoded in bitCount bits, then 7 bits per character (raw ASCII).
  // VisibleString uses the same encoding as IA5String (both encode raw ASCII value, not alphabet index).
  public static void encodeIa5String(UperOutputStream out, String value, int lb, int ub) {
    encodeCharString7bit(out, value, lb, ub);
  }

  public static String decodeIa5String(UperInputStream in, int lb, int ub) {
    return decodeCharString7bit(in, lb, ub);
  }

  public static void encodeVisibleString(UperOutputStream out, String value, int lb, int ub) {
    encodeCharString7bit(out, value, lb, ub);
  }

  public static String decodeVisibleString(UperInputStream in, int lb, int ub) {
    return decodeCharString7bit(in, lb, ub);
  }

  private static void encodeCharString7bit(UperOutputStream out, String value, int lb, int ub) {
    int range = ub - lb;
    int bitCount = Integer.SIZE - Integer.numberOfLeadingZeros(range);
    out.writeBits(value.length() - lb, bitCount);
    for (char character : value.toCharArray()) {
      out.writeBits(character, 7);
    }
  }

  private static String decodeCharString7bit(UperInputStream in, int lb, int ub) {
    int range = ub - lb;
    int bitCount = Integer.SIZE - Integer.numberOfLeadingZeros(range);
    int length = (int) in.readBits(bitCount) + lb;
    var chars = new char[length];
    for (int i = 0; i < length; i++) {
      chars[i] = (char) in.readBits(7);
    }
    return new String(chars);
  }

  // X.691 §15: BIT STRING — write/read exactly bitCount bits from/into a byte array.
  // Only a fixed bit count is supported; there is no length determinant, so a genuine
  // SIZE(lb..ub) range (lb != ub) is not wire-supported yet. Record constructors
  // therefore intentionally omit size-range validation for BIT STRING fields until this
  // is implemented — validating a range the encoder can't actually honor would be misleading.
  public static void encodeBitString(UperOutputStream out, byte[] value, int bitCount) {
    for (int i = 0; i < bitCount; i++) {
      int byteIndex = i / 8;
      int bitIndex = 7 - (i % 8);
      int bit = (value[byteIndex] >>> bitIndex) & 1;
      out.writeBits(bit, 1);
    }
  }

  public static byte[] decodeBitString(UperInputStream in, int bitCount) {
    int byteCount = (bitCount + 7) / 8;
    var result = new byte[byteCount];
    for (int i = 0; i < bitCount; i++) {
      int byteIndex = i / 8;
      int bitIndex = 7 - (i % 8);
      int bit = (int) in.readBits(1);
      if (bit == 1) {
        result[byteIndex] |= (byte) (1 << bitIndex);
      }
    }
    return result;
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
