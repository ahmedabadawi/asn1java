package io.github.ahmedabadawi.asn1java.handwritten.transport;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public final class PayloadCodec {

  // INTEGER(0..2621430): range=2621430, bits=32-Integer.numberOfLeadingZeros(2621430)=32-10=22
  private static final int MESSAGE_TIME_TO_LIVE_BITS = 22;

  public byte[] encode(Payload model) {
    var out = new UperOutputStream();
    encodeInto(out, model);
    return out.toByteArray();
  }

  void encodeInto(UperOutputStream out, Payload model) {
    out.writeBits(model.messageTimeToLive(), MESSAGE_TIME_TO_LIVE_BITS);
    UperCodecSupport.encodeOctetString(out, model.data(), 1, 65536);
  }

  public Payload decode(byte[] bytes) {
    return decodeFrom(new UperInputStream(bytes));
  }

  Payload decodeFrom(UperInputStream in) {
    int messageTimeToLive = (int) in.readBits(MESSAGE_TIME_TO_LIVE_BITS);
    byte[] data = UperCodecSupport.decodeOctetString(in, 1, 65536);
    return new Payload(messageTimeToLive, data);
  }
}
