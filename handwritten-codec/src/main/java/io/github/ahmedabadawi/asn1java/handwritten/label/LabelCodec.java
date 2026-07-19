package io.github.ahmedabadawi.asn1java.handwritten.label;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class LabelCodec {

  public byte[] encode(Label label) {
    var out = new UperOutputStream();
    UperCodecSupport.encodeUtf8String(out, label.text());
    return out.toByteArray();
  }

  public Label decode(byte[] data) {
    var in = new UperInputStream(data);
    String text = UperCodecSupport.decodeUtf8String(in);
    return new Label(text);
  }
}
