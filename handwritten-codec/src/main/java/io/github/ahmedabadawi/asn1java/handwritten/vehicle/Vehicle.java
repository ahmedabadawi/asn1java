package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

public record Vehicle(int id, Propulsion propulsion) {

  public Vehicle {
    if (id < 0) {
      throw new IllegalArgumentException("id must be >= 0");
    }
    if (propulsion == null) {
      throw new IllegalArgumentException("propulsion must not be null");
    }
  }
}
