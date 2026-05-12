package io.github.ahmedabadawi.asn1java.handwritten.person;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class PersonCodec {

    public byte[] encode(Person person) {
        if (person.name() == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        var out = new UperOutputStream();
        UperCodecSupport.encodeUtf8String(out, person.name());
        return out.toByteArray();
    }

    public Person decode(byte[] data) {
        var in = new UperInputStream(data);
        String name = UperCodecSupport.decodeUtf8String(in);
        return new Person(name);
    }
}
