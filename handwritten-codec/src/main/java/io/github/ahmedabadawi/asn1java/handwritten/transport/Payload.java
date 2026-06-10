package io.github.ahmedabadawi.asn1java.handwritten.transport;

public record Payload(int messageTimeToLive, byte[] data) {}
