package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public final class PropulsionCodec {

  private final GasEngineCodec gasEngineCodec = new GasEngineCodec();
  private final ElectricMotorCodec electricMotorCodec = new ElectricMotorCodec();

  public byte[] encode(Propulsion model) {
    var out = new UperOutputStream();
    encodeInto(out, model);
    return out.toByteArray();
  }

  void encodeInto(UperOutputStream out, Propulsion model) {
    switch (model) {
      case Propulsion.Gasoline gasoline -> {
        out.writeBits(0, 2);
        gasEngineCodec.encodeInto(out, gasoline.value());
      }
      case Propulsion.Electric electric -> {
        out.writeBits(1, 2);
        electricMotorCodec.encodeInto(out, electric.value());
      }
      case Propulsion.None ignored -> out.writeBits(2, 2);
    }
  }

  public Propulsion decode(byte[] data) {
    return decodeFrom(new UperInputStream(data));
  }

  Propulsion decodeFrom(UperInputStream in) {
    var index = (int) in.readBits(2);
    return switch (index) {
      case 0 -> new Propulsion.Gasoline(gasEngineCodec.decodeFrom(in));
      case 1 -> new Propulsion.Electric(electricMotorCodec.decodeFrom(in));
      case 2 -> new Propulsion.None();
      default -> throw new IllegalArgumentException("Unknown Propulsion choice index: " + index);
    };
  }
}
