package io.github.ahmedabadawi.asn1java.handwritten.person;

public record Person(String name) {

  public Person {
    if (name == null) {
      throw new IllegalArgumentException("name must not be null");
    }
  }
}
