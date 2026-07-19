package io.github.ahmedabadawi.asn1java.handwritten.profile;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class ProfileCodec {

  private static final int STATUS_BIT_COUNT = 2;
  private static final int STATUS_DEFAULT = 1;
  private static final String NICKNAME_DEFAULT = "anonymous";

  public byte[] encode(Profile model) {
    var out = new UperOutputStream();
    out.writeBits(model.status() != STATUS_DEFAULT ? 1 : 0, 1);
    out.writeBits(!model.nickname().equals(NICKNAME_DEFAULT) ? 1 : 0, 1);
    out.writeBits(model.id(), 8);
    if (model.status() != STATUS_DEFAULT) {
      out.writeBits(model.status(), STATUS_BIT_COUNT);
    }
    if (!model.nickname().equals(NICKNAME_DEFAULT)) {
      UperCodecSupport.encodeUtf8String(out, model.nickname());
    }
    return out.toByteArray();
  }

  public Profile decode(byte[] data) {
    var in = new UperInputStream(data);
    var statusPresent = in.readBits(1) != 0;
    var nicknamePresent = in.readBits(1) != 0;
    var id = (int) in.readBits(8);
    int status = statusPresent ? (int) in.readBits(STATUS_BIT_COUNT) : STATUS_DEFAULT;
    String nickname = nicknamePresent ? UperCodecSupport.decodeUtf8String(in) : NICKNAME_DEFAULT;
    return new Profile(id, status, nickname);
  }
}
