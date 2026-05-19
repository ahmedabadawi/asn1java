package io.github.ahmedabadawi.asn1java.handwritten.score;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class ScoreCodec {

  public byte[] encode(Score score) {
    if (score.level() < 1) {
      throw new IllegalArgumentException("level must be >= 1");
    }
    if (score.level() > 10) {
      throw new IllegalArgumentException("level must be <= 10");
    }
    if (score.points() < 0) {
      throw new IllegalArgumentException("points must be >= 0");
    }
    if (score.points() > 999) {
      throw new IllegalArgumentException("points must be <= 999");
    }
    if (score.offset() < -10) {
      throw new IllegalArgumentException("offset must be >= -10");
    }
    if (score.offset() > 10) {
      throw new IllegalArgumentException("offset must be <= 10");
    }
    var out = new UperOutputStream();
    out.writeBits(score.level() - 1, 4);
    out.writeBits(score.points(), 10);
    out.writeBits(score.offset() + 10, 5);
    return out.toByteArray();
  }

  public Score decode(byte[] data) {
    var in = new UperInputStream(data);
    int level = (int) in.readBits(4) + 1;
    int points = (int) in.readBits(10);
    int offset = (int) in.readBits(5) - 10;
    return new Score(level, points, offset);
  }
}
