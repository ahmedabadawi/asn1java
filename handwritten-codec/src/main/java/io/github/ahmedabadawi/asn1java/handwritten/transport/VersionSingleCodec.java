package io.github.ahmedabadawi.asn1java.handwritten.transport;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public final class VersionSingleCodec {

  public byte[] encode(VersionSingle model) {
    var out = new UperOutputStream();
    encodeInto(out, model);
    return out.toByteArray();
  }

  void encodeInto(UperOutputStream out, VersionSingle model) {
    out.writeBits(model.value(), 8);
  }

  public VersionSingle decode(byte[] data) {
    return decodeFrom(new UperInputStream(data));
  }

  VersionSingle decodeFrom(UperInputStream in) {
    int value = (int) in.readBits(8);
    return new VersionSingle(value);
  }
}
