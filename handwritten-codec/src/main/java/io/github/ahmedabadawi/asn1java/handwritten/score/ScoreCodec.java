package io.github.ahmedabadawi.asn1java.handwritten.score;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class ScoreCodec {

  public byte[] encode(Score score) {
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
