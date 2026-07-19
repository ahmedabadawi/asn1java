package io.github.ahmedabadawi.asn1java.handwritten.transport;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public final class MessageTypeCodec {

  public byte[] encode(MessageType model) {
    var out = new UperOutputStream();
    encodeInto(out, model);
    return out.toByteArray();
  }

  void encodeInto(UperOutputStream out, MessageType model) {
    UperCodecSupport.encodeUtf8String(out, model.value());
  }

  public MessageType decode(byte[] data) {
    return decodeFrom(new UperInputStream(data));
  }

  MessageType decodeFrom(UperInputStream in) {
    return new MessageType(UperCodecSupport.decodeUtf8String(in));
  }
}
