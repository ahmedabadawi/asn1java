package io.github.ahmedabadawi.asn1java.handwritten.status;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class StatusCodec {

  private static final int STATE_BIT_COUNT = 2;

  public byte[] encode(Status status) {
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
