package io.github.ahmedabadawi.asn1java.handwritten.label;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;
import java.nio.charset.StandardCharsets;

public class LabelCodec {

  private static final int TEXT_SIZE_LB = 1;
  private static final int TEXT_SIZE_UB = 64;

  public byte[] encode(Label label) {
    if (label.text() == null) {
      throw new IllegalArgumentException("text must not be null");
    }
    int byteLength = label.text().getBytes(StandardCharsets.UTF_8).length;
    if (byteLength < TEXT_SIZE_LB) {
      throw new IllegalArgumentException(
          "text length must be >= %d".formatted(TEXT_SIZE_LB));
    }
    if (byteLength > TEXT_SIZE_UB) {
      throw new IllegalArgumentException(
          "text length must be <= %d".formatted(TEXT_SIZE_UB));
    }
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
