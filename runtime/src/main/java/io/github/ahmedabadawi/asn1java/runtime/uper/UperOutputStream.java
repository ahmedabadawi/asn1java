package io.github.ahmedabadawi.asn1java.runtime.uper;

import java.io.ByteArrayOutputStream;

public class UperOutputStream {

  private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
  private int accumulator = 0;
  private int bitsInAccumulator = 0;

  public void writeBits(long value, int numBits) {
    for (int i = numBits - 1; i >= 0; i--) {
      int bit = (int) ((value >>> i) & 1);
      accumulator = (accumulator << 1) | bit;
      bitsInAccumulator++;
      if (bitsInAccumulator == 8) {
        bytes.write(accumulator);
        accumulator = 0;
        bitsInAccumulator = 0;
      }
    }
  }

  public byte[] toByteArray() {
    if (bitsInAccumulator > 0) {
      bytes.write(accumulator << (8 - bitsInAccumulator));
    }
    return bytes.toByteArray();
  }
}
