package io.github.ahmedabadawi.asn1java.handwritten.transport;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

import java.nio.charset.StandardCharsets;

public final class MessageTypeCodec {

  public byte[] encode(MessageType model) {
    var out = new UperOutputStream();
    encodeInto(out, model);
    return out.toByteArray();
  }

  void encodeInto(UperOutputStream out, MessageType model) {
    if (model.value() == null) {
      throw new IllegalArgumentException("value must not be null");
    }
    int byteLength = model.value().getBytes(StandardCharsets.UTF_8).length;
    if (byteLength < 2) {
      throw new IllegalArgumentException("value length must be >= 2");
    }
    if (byteLength > 9) {
      throw new IllegalArgumentException("value length must be <= 9");
    }
    UperCodecSupport.encodeUtf8String(out, model.value());
  }

  public MessageType decode(byte[] data) {
    return decodeFrom(new UperInputStream(data));
  }

  MessageType decodeFrom(UperInputStream in) {
    return new MessageType(UperCodecSupport.decodeUtf8String(in));
  }
}
