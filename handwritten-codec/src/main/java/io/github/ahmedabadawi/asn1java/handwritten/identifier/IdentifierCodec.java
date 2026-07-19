package io.github.ahmedabadawi.asn1java.handwritten.identifier;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class IdentifierCodec {

  private static final int CODE_SIZE_LB = 1;
  private static final int CODE_SIZE_UB = 8;
  private static final int LABEL_SIZE_LB = 1;
  private static final int LABEL_SIZE_UB = 8;

  public byte[] encode(Identifier identifier) {
    var out = new UperOutputStream();
    UperCodecSupport.encodeIa5String(out, identifier.code(), CODE_SIZE_LB, CODE_SIZE_UB);
    UperCodecSupport.encodeVisibleString(out, identifier.label(), LABEL_SIZE_LB, LABEL_SIZE_UB);
    return out.toByteArray();
  }

  public Identifier decode(byte[] data) {
    var in = new UperInputStream(data);
    String code = UperCodecSupport.decodeIa5String(in, CODE_SIZE_LB, CODE_SIZE_UB);
    String label = UperCodecSupport.decodeVisibleString(in, LABEL_SIZE_LB, LABEL_SIZE_UB);
    return new Identifier(code, label);
  }
}
