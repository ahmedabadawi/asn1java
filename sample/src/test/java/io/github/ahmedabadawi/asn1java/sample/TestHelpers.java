package io.github.ahmedabadawi.asn1java.sample;

public final class TestHelpers {
  private TestHelpers() {

  }

  public static String toHex(byte[] bytes) {
    var sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append("%02x".formatted(b));
    }
    return sb.toString();
  }

  public static byte[] fromHex(String hex) {
    byte[] bytes = new byte[hex.length() / 2];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) Integer.parseInt(hex, i * 2, i * 2 + 2, 16);
    }
    return bytes;
  }
}
