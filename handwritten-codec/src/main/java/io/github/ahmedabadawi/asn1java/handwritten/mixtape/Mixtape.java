package io.github.ahmedabadawi.asn1java.handwritten.mixtape;

import io.github.ahmedabadawi.asn1java.handwritten.playlist.Track;

import java.util.List;

public record Mixtape(String curator, List<Track> tracks) {

  public Mixtape {
    if (curator == null) {
      throw new IllegalArgumentException("curator must not be null");
    }
    if (tracks == null) {
      throw new IllegalArgumentException("tracks must not be null");
    }
    if (tracks.size() < 1 || tracks.size() > 10) {
      throw new IllegalArgumentException("tracks must have between 1 and 10 elements");
    }
  }
}
