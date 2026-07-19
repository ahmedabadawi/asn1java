package io.github.ahmedabadawi.asn1java.handwritten.contact;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class ContactCodec {

  public byte[] encode(Contact model) {
    var out = new UperOutputStream();
    out.writeBits(model.age() != null ? 1 : 0, 1);
    out.writeBits(model.id(), 8);
    if (model.age() != null) {
      out.writeBits(model.age(), 8);
    }
    return out.toByteArray();
  }

  public Contact decode(byte[] data) {
    var in = new UperInputStream(data);
    var agePresent = in.readBits(1) != 0;
    var id = (int) in.readBits(8);
    Integer age = agePresent ? (int) in.readBits(8) : null;
    return new Contact(id, age);
  }
}
