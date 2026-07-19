package io.github.ahmedabadawi.asn1java.handwritten.packet;

public record Packet(byte[] payload) {

  private static final int PAYLOAD_SIZE_LB = 1;
  private static final int PAYLOAD_SIZE_UB = 4;

  public Packet {
    if (payload == null) {
      throw new IllegalArgumentException("payload must not be null");
    }
    int length = payload.length;
    if (length < PAYLOAD_SIZE_LB || length > PAYLOAD_SIZE_UB) {
      throw new IllegalArgumentException(
          "payload length must be in range %d..%d".formatted(PAYLOAD_SIZE_LB, PAYLOAD_SIZE_UB));
    }
  }
}
