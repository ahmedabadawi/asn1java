package io.github.ahmedabadawi.asn1java.handwritten.transport;

public record Message(ProtocolVersion protocolVersion, MessageType messageType, Payload payload) {}
