package io.github.ahmedabadawi.asn1java.handwritten.packet;

import io.github.ahmedabadawi.asn1java.runtime.uper.UperCodecSupport;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperInputStream;
import io.github.ahmedabadawi.asn1java.runtime.uper.UperOutputStream;

public class PacketCodec {

  private static final int PAYLOAD_SIZE_LB = 1;
  private static final int PAYLOAD_SIZE_UB = 4;

  public byte[] encode(Packet packet) {
    var out = new UperOutputStream();
    UperCodecSupport.encodeOctetString(out, packet.payload(), PAYLOAD_SIZE_LB, PAYLOAD_SIZE_UB);
    return out.toByteArray();
  }

  public Packet decode(byte[] data) {
    var in = new UperInputStream(data);
    byte[] payload = UperCodecSupport.decodeOctetString(in, PAYLOAD_SIZE_LB, PAYLOAD_SIZE_UB);
    return new Packet(payload);
  }
}
