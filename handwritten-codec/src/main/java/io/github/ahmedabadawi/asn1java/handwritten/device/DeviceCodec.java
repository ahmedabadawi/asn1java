package io.github.ahmedabadawi.asn1java.handwritten.device;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class DeviceCodec {

    public byte[] encode(Device device) {
        var out = new UperOutputStream();
        out.writeBits(device.active() ? 1 : 0, 1);
        return out.toByteArray();
    }

    public Device decode(byte[] data) {
        var in = new UperInputStream(data);
        boolean active = in.readBits(1) != 0;
        return new Device(active);
    }
}
