package io.github.ahmedabadawi.asn1java.handwritten.limits;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public final class SettingsCodec {

  public byte[] encode(Settings model) {
    var out = new UperOutputStream();
    encodeInto(out, model);
    return out.toByteArray();
  }

  void encodeInto(UperOutputStream out, Settings model) {
    out.writeBits(model.volume(), 7);
    UperCodecSupport.encodeSequenceOf(out, model.tags(), 0, 10, UperCodecSupport::encodeUtf8String);
  }

  public Settings decode(byte[] data) {
    return decodeFrom(new UperInputStream(data));
  }

  Settings decodeFrom(UperInputStream in) {
    int volume = (int) in.readBits(7);
    var tags = UperCodecSupport.decodeSequenceOf(in, 0, 10, UperCodecSupport::decodeUtf8String);
    return new Settings(volume, tags);
  }
}
