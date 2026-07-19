package io.github.ahmedabadawi.asn1java.handwritten.transport;

public record Message(ProtocolVersion protocolVersion, MessageType messageType, Payload payload) {

  public Message {
    if (protocolVersion == null) {
      throw new IllegalArgumentException("protocolVersion must not be null");
    }
    if (messageType == null) {
      throw new IllegalArgumentException("messageType must not be null");
    }
    if (payload == null) {
      throw new IllegalArgumentException("payload must not be null");
    }
  }
}
