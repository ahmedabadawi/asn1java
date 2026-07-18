package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public final class ElectricMotorCodec {

  public byte[] encode(ElectricMotor model) {
    var out = new UperOutputStream();
    encodeInto(out, model);
    return out.toByteArray();
  }

  void encodeInto(UperOutputStream out, ElectricMotor model) {
    if (model.powerKw() < 0) {
      throw new IllegalArgumentException("powerKw must be >= 0");
    }
    if (model.batteryKwh() < 0) {
      throw new IllegalArgumentException("batteryKwh must be >= 0");
    }
    out.writeBits(model.powerKw(), 10);
    out.writeBits(model.batteryKwh(), 9);
  }

  public ElectricMotor decode(byte[] data) {
    return decodeFrom(new UperInputStream(data));
  }

  ElectricMotor decodeFrom(UperInputStream in) {
    var powerKw = (int) in.readBits(10);
    var batteryKwh = (int) in.readBits(9);
    return new ElectricMotor(powerKw, batteryKwh);
  }
}
