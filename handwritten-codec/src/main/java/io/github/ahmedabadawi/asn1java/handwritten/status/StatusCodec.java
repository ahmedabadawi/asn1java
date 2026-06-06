package io.github.ahmedabadawi.asn1java.handwritten.status;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class StatusCodec {

  private static final int STATE_BIT_COUNT = 2;
  private static final int STATE_MAX = 2;

  public byte[] encode(Status status) {
    if (status.state() < 0 || status.state() > STATE_MAX) {
      throw new IllegalArgumentException(
          "state must be in range 0..%d".formatted(STATE_MAX));
    }
    var out = new UperOutputStream();
    out.writeBits(status.state(), STATE_BIT_COUNT);
    return out.toByteArray();
  }

  public Status decode(byte[] data) {
    var in = new UperInputStream(data);
    int state = (int) in.readBits(STATE_BIT_COUNT);
    return new Status(state);
  }
}
