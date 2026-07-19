package io.github.ahmedabadawi.asn1java.handwritten.vehicle;

public record Vehicle(int id, Propulsion propulsion) {

  private static final int ID_MAX = 65535;

  public Vehicle {
    if (id < 0) {
      throw new IllegalArgumentException("id must be >= 0");
    }
    if (id > ID_MAX) {
      throw new IllegalArgumentException("id must be <= %d".formatted(ID_MAX));
    }
    if (propulsion == null) {
      throw new IllegalArgumentException("propulsion must not be null");
    }
  }
}
