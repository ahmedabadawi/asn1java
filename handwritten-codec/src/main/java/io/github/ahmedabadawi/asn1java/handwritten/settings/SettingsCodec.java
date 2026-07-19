package io.github.ahmedabadawi.asn1java.handwritten.settings;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class SettingsCodec {

  private static final int VOLUME_DEFAULT = 50;
  private static final boolean MUTED_DEFAULT = false;

  public byte[] encode(Settings model) {
    var out = new UperOutputStream();
    out.writeBits(model.volume() != VOLUME_DEFAULT ? 1 : 0, 1);
    out.writeBits(model.muted() != MUTED_DEFAULT ? 1 : 0, 1);
    out.writeBits(model.id(), 8);
    if (model.volume() != VOLUME_DEFAULT) {
      out.writeBits(model.volume(), 7);
    }
    if (model.muted() != MUTED_DEFAULT) {
      out.writeBits(model.muted() ? 1 : 0, 1);
    }
    return out.toByteArray();
  }

  public Settings decode(byte[] data) {
    var in = new UperInputStream(data);
    var volumePresent = in.readBits(1) != 0;
    var mutedPresent = in.readBits(1) != 0;
    var id = (int) in.readBits(8);
    int volume = volumePresent ? (int) in.readBits(7) : VOLUME_DEFAULT;
    boolean muted = mutedPresent ? in.readBits(1) != 0 : MUTED_DEFAULT;
    return new Settings(id, volume, muted);
  }
}
