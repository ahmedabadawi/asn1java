package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public final class GasEngineCodec {

  public byte[] encode(GasEngine model) {
    var out = new UperOutputStream();
    encodeInto(out, model);
    return out.toByteArray();
  }

  void encodeInto(UperOutputStream out, GasEngine model) {
    out.writeBits(model.displacementCc(), 13);
    out.writeBits(model.cylinders() - 1, 4);
  }

  public GasEngine decode(byte[] data) {
    return decodeFrom(new UperInputStream(data));
  }

  GasEngine decodeFrom(UperInputStream in) {
    var displacementCc = (int) in.readBits(13);
    var cylinders = (int) in.readBits(4) + 1;
    return new GasEngine(displacementCc, cylinders);
  }
}
