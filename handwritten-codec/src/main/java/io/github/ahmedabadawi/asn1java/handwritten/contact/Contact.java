package io.github.ahmedabadawi.asn1java.handwritten.contact;

public record Contact(int id, Integer age) {

  public Contact {
    if (id < 0) {
      throw new IllegalArgumentException("id must be >= 0");
    }
    if (id > 255) {
      throw new IllegalArgumentException("id must be <= 255");
    }
    if (age != null && age < 0) {
      throw new IllegalArgumentException("age must be >= 0");
    }
    if (age != null && age > 255) {
      throw new IllegalArgumentException("age must be <= 255");
    }
  }
}
