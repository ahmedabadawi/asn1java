package io.github.ahmedabadawi.asn1java.handwritten.playlist;

import java.util.List;

public record Playlist(List<String> tags, List<Track> tracks, List<Track> topThree) {

  public Playlist {
    if (tags == null) {
      throw new IllegalArgumentException("tags must not be null");
    }
    if (tracks == null) {
      throw new IllegalArgumentException("tracks must not be null");
    }
    if (tracks.size() < 1 || tracks.size() > 64) {
      throw new IllegalArgumentException("tracks must have between 1 and 64 elements");
    }
    if (topThree == null) {
      throw new IllegalArgumentException("topThree must not be null");
    }
    if (topThree.size() != 3) {
      throw new IllegalArgumentException("topThree must have exactly 3 elements");
    }
  }
}
