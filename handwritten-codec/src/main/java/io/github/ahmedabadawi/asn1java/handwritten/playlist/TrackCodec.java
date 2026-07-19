package io.github.ahmedabadawi.asn1java.handwritten.playlist;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public final class TrackCodec {

  public byte[] encode(Track model) {
    var out = new UperOutputStream();
    encodeInto(out, model);
    return out.toByteArray();
  }

  void encodeInto(UperOutputStream out, Track model) {
    UperCodecSupport.encodeUtf8String(out, model.title());
  }

  public Track decode(byte[] data) {
    return decodeFrom(new UperInputStream(data));
  }

  Track decodeFrom(UperInputStream in) {
    return new Track(UperCodecSupport.decodeUtf8String(in));
  }
}
