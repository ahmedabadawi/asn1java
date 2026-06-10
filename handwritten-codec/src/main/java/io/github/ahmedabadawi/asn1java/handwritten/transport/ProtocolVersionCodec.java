package io.github.ahmedabadawi.asn1java.handwritten.transport;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public final class ProtocolVersionCodec {

  private final VersionSingleCodec versionSingleCodec = new VersionSingleCodec();

  public byte[] encode(ProtocolVersion model) {
    var out = new UperOutputStream();
    encodeInto(out, model);
    return out.toByteArray();
  }

  void encodeInto(UperOutputStream out, ProtocolVersion model) {
    versionSingleCodec.encodeInto(out, model.major());
    versionSingleCodec.encodeInto(out, model.minor());
  }

  public ProtocolVersion decode(byte[] data) {
    return decodeFrom(new UperInputStream(data));
  }

  ProtocolVersion decodeFrom(UperInputStream in) {
    var major = versionSingleCodec.decodeFrom(in);
    var minor = versionSingleCodec.decodeFrom(in);
    return new ProtocolVersion(major, minor);
  }
}
