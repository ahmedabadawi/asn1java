package io.github.ahmedabadawi.asn1java.handwritten.playlist;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public final class PlaylistCodec {

  private final TrackCodec trackCodec = new TrackCodec();

  public byte[] encode(Playlist model) {
    var out = new UperOutputStream();
    encodeInto(out, model);
    return out.toByteArray();
  }

  void encodeInto(UperOutputStream out, Playlist model) {
    UperCodecSupport.encodeSequenceOf(out, model.tags(), 0, Long.MAX_VALUE,
        UperCodecSupport::encodeUtf8String);
    UperCodecSupport.encodeSequenceOf(out, model.tracks(), 1, 64, trackCodec::encodeInto);
    UperCodecSupport.encodeSequenceOf(out, model.topThree(), 3, 3, trackCodec::encodeInto);
  }

  public Playlist decode(byte[] data) {
    return decodeFrom(new UperInputStream(data));
  }

  Playlist decodeFrom(UperInputStream in) {
    var tags = UperCodecSupport.decodeSequenceOf(in, 0, Long.MAX_VALUE,
        UperCodecSupport::decodeUtf8String);
    var tracks = UperCodecSupport.decodeSequenceOf(in, 1, 64, trackCodec::decodeFrom);
    var topThree = UperCodecSupport.decodeSequenceOf(in, 3, 3, trackCodec::decodeFrom);
    return new Playlist(tags, tracks, topThree);
  }
}
