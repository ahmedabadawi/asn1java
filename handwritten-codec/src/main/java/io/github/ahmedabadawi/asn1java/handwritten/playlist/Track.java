package io.github.ahmedabadawi.asn1java.handwritten.playlist;

public record Track(String title) {

  public Track {
    if (title == null) {
      throw new IllegalArgumentException("title must not be null");
    }
  }
}
