package io.github.ahmedabadawi.asn1java.handwritten.transport;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public final class MessageCodec {

  private final ProtocolVersionCodec protocolVersionCodec = new ProtocolVersionCodec();
  private final MessageTypeCodec messageTypeCodec = new MessageTypeCodec();
  private final PayloadCodec payloadCodec = new PayloadCodec();

  public byte[] encode(Message model) {
    var out = new UperOutputStream();
    encodeInto(out, model);
    return out.toByteArray();
  }

  void encodeInto(UperOutputStream out, Message model) {
    protocolVersionCodec.encodeInto(out, model.protocolVersion());
    messageTypeCodec.encodeInto(out, model.messageType());
    payloadCodec.encodeInto(out, model.payload());
  }

  public Message decode(byte[] data) {
    return decodeFrom(new UperInputStream(data));
  }

  Message decodeFrom(UperInputStream in) {
    var protocolVersion = protocolVersionCodec.decodeFrom(in);
    var messageType = messageTypeCodec.decodeFrom(in);
    var payload = payloadCodec.decodeFrom(in);
    return new Message(protocolVersion, messageType, payload);
  }
}
