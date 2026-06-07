package io.github.ahmedabadawi.asn1java.handwritten.priority;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class PriorityCodec {

  private static final int LEVEL_BIT_COUNT = 2;
  private static final int LEVEL_MAX = 2;
  private static final long ADJUSTMENT_MAX = 0;

  public byte[] encode(Priority priority) {
    if (priority.level() < 0 || priority.level() > LEVEL_MAX) {
      throw new IllegalArgumentException(
          "level must be in range 0..%d".formatted(LEVEL_MAX));
    }
    if (priority.adjustment() > ADJUSTMENT_MAX) {
      throw new IllegalArgumentException(
          "adjustment must be <= %d".formatted(ADJUSTMENT_MAX));
    }
    var out = new UperOutputStream();
    out.writeBits(priority.level(), LEVEL_BIT_COUNT);
    UperCodecSupport.encodeUnconstrainedInt(out, priority.adjustment());
    return out.toByteArray();
  }

  public Priority decode(byte[] data) {
    var in = new UperInputStream(data);
    int level = (int) in.readBits(LEVEL_BIT_COUNT);
    long adjustment = UperCodecSupport.decodeUnconstrainedInt(in);
    return new Priority(level, adjustment);
  }
}
