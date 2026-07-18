package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

public sealed interface Propulsion permits Propulsion.Gasoline, Propulsion.Electric, Propulsion.None {

  record Gasoline(GasEngine value) implements Propulsion {}

  record Electric(ElectricMotor value) implements Propulsion {}

  record None() implements Propulsion {}
}
