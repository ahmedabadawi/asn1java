package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

public sealed interface Propulsion permits Propulsion.Gasoline, Propulsion.Electric, Propulsion.None {

  record Gasoline(GasEngine value) implements Propulsion {
    public Gasoline {
      if (value == null) {
        throw new IllegalArgumentException("value must not be null");
      }
    }
  }

  record Electric(ElectricMotor value) implements Propulsion {
    public Electric {
      if (value == null) {
        throw new IllegalArgumentException("value must not be null");
      }
    }
  }

  record None() implements Propulsion {}
}
