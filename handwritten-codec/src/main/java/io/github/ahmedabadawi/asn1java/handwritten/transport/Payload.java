package io.github.ahmedabadawi.asn1java.handwritten.transport;

public record Payload(int messageTimeToLive, byte[] data) {

  public Payload {
    if (messageTimeToLive < 0) {
      throw new IllegalArgumentException("messageTimeToLive must be >= 0");
    }
    if (messageTimeToLive > 2621430) {
      throw new IllegalArgumentException("messageTimeToLive must be <= 2621430");
    }
    if (data == null) {
      throw new IllegalArgumentException("data must not be null");
    }
    if (data.length < 1) {
      throw new IllegalArgumentException("data length must be >= 1");
    }
    if (data.length > 65536) {
      throw new IllegalArgumentException("data length must be <= 65536");
    }
  }
}
