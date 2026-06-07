package io.github.ahmedabadawi.asn1java.handwritten.flags;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class FlagsCodec {

  private static final int BITS_SIZE = 8;

  public byte[] encode(Flags flags) {
    if (flags.bits() == null) {
      throw new IllegalArgumentException("bits must not be null");
    }
    if (flags.bits().length * 8 != BITS_SIZE) {
      throw new IllegalArgumentException(
          "bits must be exactly %d bits (%d bytes)".formatted(BITS_SIZE, BITS_SIZE / 8));
    }
    var out = new UperOutputStream();
    UperCodecSupport.encodeBitString(out, flags.bits(), BITS_SIZE);
    return out.toByteArray();
  }

  public Flags decode(byte[] data) {
    var in = new UperInputStream(data);
    byte[] bits = UperCodecSupport.decodeBitString(in, BITS_SIZE);
    return new Flags(bits);
  }
}
