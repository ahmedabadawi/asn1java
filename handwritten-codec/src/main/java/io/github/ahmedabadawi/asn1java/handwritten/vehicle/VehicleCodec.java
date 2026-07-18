package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public final class VehicleCodec {

  private final PropulsionCodec propulsionCodec = new PropulsionCodec();

  public byte[] encode(Vehicle model) {
    var out = new UperOutputStream();
    encodeInto(out, model);
    return out.toByteArray();
  }

  void encodeInto(UperOutputStream out, Vehicle model) {
    if (model.id() < 0) {
      throw new IllegalArgumentException("id must be >= 0");
    }
    out.writeBits(model.id(), 16);
    propulsionCodec.encodeInto(out, model.propulsion());
  }

  public Vehicle decode(byte[] data) {
    return decodeFrom(new UperInputStream(data));
  }

  Vehicle decodeFrom(UperInputStream in) {
    var id = (int) in.readBits(16);
    var propulsion = propulsionCodec.decodeFrom(in);
    return new Vehicle(id, propulsion);
  }
}
