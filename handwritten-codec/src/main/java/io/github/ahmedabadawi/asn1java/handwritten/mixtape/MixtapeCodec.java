package io.github.ahmedabadawi.asn1java.handwritten.mixtape;

import io.github.ahmedabadawi.asn1java.handwritten.playlist.TrackCodec;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public final class MixtapeCodec {

  private final TrackCodec trackCodec = new TrackCodec();

  public byte[] encode(Mixtape model) {
    var out = new UperOutputStream();
    encodeInto(out, model);
    return out.toByteArray();
  }

  void encodeInto(UperOutputStream out, Mixtape model) {
    UperCodecSupport.encodeUtf8String(out, model.curator());
    UperCodecSupport.encodeSequenceOf(out, model.tracks(), 1, 10, trackCodec::encodeInto);
  }

  public Mixtape decode(byte[] data) {
    return decodeFrom(new UperInputStream(data));
  }

  Mixtape decodeFrom(UperInputStream in) {
    var curator = UperCodecSupport.decodeUtf8String(in);
    var tracks = UperCodecSupport.decodeSequenceOf(in, 1, 10, trackCodec::decodeFrom);
    return new Mixtape(curator, tracks);
  }
}
