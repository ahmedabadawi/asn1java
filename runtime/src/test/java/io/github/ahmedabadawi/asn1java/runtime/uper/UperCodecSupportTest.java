package io.github.ahmedabadawi.asn1java.runtime.uper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class UperCodecSupportTest {

  private long roundTrip(long value) {
    var out = new UperOutputStream();
    UperCodecSupport.encodeSemiConstrainedInt(out, value);
    var in = new UperInputStream(out.toByteArray());
    return UperCodecSupport.decodeSemiConstrainedInt(in);
  }

  @Test
  void roundTrip_zero() {
    assertThat(roundTrip(0)).isEqualTo(0L);
  }

  @Test
  void roundTrip_one() {
    assertThat(roundTrip(1)).isEqualTo(1L);
  }

  @Test
  void roundTrip_255() {
    assertThat(roundTrip(255)).isEqualTo(255L);
  }

  @Test
  void roundTrip_256() {
    assertThat(roundTrip(256)).isEqualTo(256L);
  }

  @Test
  void roundTrip_largeValue() {
    assertThat(roundTrip(100_000)).isEqualTo(100_000L);
  }

  @Test
  void encode_zero_producesTwoBytes() {
    // length=1 (0x01), value=0x00
    var out = new UperOutputStream();
    UperCodecSupport.encodeSemiConstrainedInt(out, 0);
    assertThat(out.toByteArray()).isEqualTo(new byte[] {0x01, 0x00});
  }

  @Test
  void encode_one_producesTwoBytes() {
    // length=1 (0x01), value=0x01
    var out = new UperOutputStream();
    UperCodecSupport.encodeSemiConstrainedInt(out, 1);
    assertThat(out.toByteArray()).isEqualTo(new byte[] {0x01, 0x01});
  }

  private String roundTripString(String value) {
    var out = new UperOutputStream();
    UperCodecSupport.encodeUtf8String(out, value);
    var in = new UperInputStream(out.toByteArray());
    return UperCodecSupport.decodeUtf8String(in);
  }

  @Test
  void encodeUtf8String_WhenEmpty_ShouldRoundTrip() {
    assertThat(roundTripString("")).isEqualTo("");
  }

  @Test
  void encodeUtf8String_WhenAscii_ShouldRoundTrip() {
    assertThat(roundTripString("hello")).isEqualTo("hello");
  }

  @Test
  void encodeUtf8String_WhenEmpty_ShouldProduceSingleZeroByte() {
    // §10.7: empty string → length byte 0x00, no payload
    var out = new UperOutputStream();
    UperCodecSupport.encodeUtf8String(out, "");
    assertThat(out.toByteArray()).isEqualTo(new byte[] {0x00});
  }

  @Test
  void encodeUtf8String_WhenHello_ShouldMatchOracleHex() {
    // oracle: "hello" → 0568656c6c6f
    var out = new UperOutputStream();
    UperCodecSupport.encodeUtf8String(out, "hello");
    var sb = new StringBuilder();
    for (byte b : out.toByteArray()) sb.append("%02x".formatted(b));
    assertThat(sb.toString()).isEqualTo("0568656c6c6f");
  }

  private String hex(byte[] bytes) {
    var sb = new StringBuilder();
    for (byte b : bytes) sb.append("%02x".formatted(b));
    return sb.toString();
  }

  @Test
  void encodeSequenceOf_WhenUnconstrainedAndEmpty_ShouldProduceSingleZeroByte() {
    var out = new UperOutputStream();
    UperCodecSupport.encodeSequenceOf(out, List.<Integer>of(), 0, Long.MAX_VALUE,
        (stream, item) -> stream.writeBits(item, 8));
    assertThat(hex(out.toByteArray())).isEqualTo("00");
  }

  @Test
  void encodeSequenceOf_WhenUnconstrained_ShouldMatchWorkedExample() {
    var out = new UperOutputStream();
    UperCodecSupport.encodeSequenceOf(out, List.of(1, 2), 0, Long.MAX_VALUE,
        (stream, item) -> stream.writeBits(item, 8));
    assertThat(hex(out.toByteArray())).isEqualTo("020102");
  }

  @Test
  void decodeSequenceOf_WhenUnconstrained_ShouldRoundTrip() {
    var out = new UperOutputStream();
    UperCodecSupport.encodeSequenceOf(out, List.of(1, 2), 0, Long.MAX_VALUE,
        (stream, item) -> stream.writeBits(item, 8));
    var in = new UperInputStream(out.toByteArray());
    List<Integer> decoded = UperCodecSupport.decodeSequenceOf(in, 0, Long.MAX_VALUE,
        stream -> (int) stream.readBits(8));
    assertThat(decoded).containsExactly(1, 2);
  }

  @Test
  void encodeSequenceOf_WhenRangeConstrainedSingleElement_ShouldMatchWorkedExample() {
    // SEQUENCE (SIZE (1..4)) OF INTEGER (0..255): range=3, bitCount=2
    var out = new UperOutputStream();
    UperCodecSupport.encodeSequenceOf(out, List.of(7), 1, 4,
        (stream, item) -> stream.writeBits(item, 8));
    assertThat(hex(out.toByteArray())).isEqualTo("01c0");
  }

  @Test
  void encodeSequenceOf_WhenRangeConstrainedTwoElements_ShouldMatchWorkedExample() {
    var out = new UperOutputStream();
    UperCodecSupport.encodeSequenceOf(out, List.of(7, 9), 1, 4,
        (stream, item) -> stream.writeBits(item, 8));
    assertThat(hex(out.toByteArray())).isEqualTo("41c240");
  }

  @Test
  void decodeSequenceOf_WhenRangeConstrained_ShouldRoundTrip() {
    var out = new UperOutputStream();
    UperCodecSupport.encodeSequenceOf(out, List.of(7, 9), 1, 4,
        (stream, item) -> stream.writeBits(item, 8));
    var in = new UperInputStream(out.toByteArray());
    List<Integer> decoded = UperCodecSupport.decodeSequenceOf(in, 1, 4,
        stream -> (int) stream.readBits(8));
    assertThat(decoded).containsExactly(7, 9);
  }

  @Test
  void encodeSequenceOf_WhenFixedSize_ShouldWriteNoLengthBits() {
    // SEQUENCE (SIZE (2..2)) OF INTEGER (0..255): no length determinant
    var out = new UperOutputStream();
    UperCodecSupport.encodeSequenceOf(out, List.of(5, 6), 2, 2,
        (stream, item) -> stream.writeBits(item, 8));
    assertThat(hex(out.toByteArray())).isEqualTo("0506");
  }

  @Test
  void decodeSequenceOf_WhenFixedSize_ShouldRoundTrip() {
    var out = new UperOutputStream();
    UperCodecSupport.encodeSequenceOf(out, List.of(5, 6), 2, 2,
        (stream, item) -> stream.writeBits(item, 8));
    var in = new UperInputStream(out.toByteArray());
    List<Integer> decoded = UperCodecSupport.decodeSequenceOf(in, 2, 2,
        stream -> (int) stream.readBits(8));
    assertThat(decoded).containsExactly(5, 6);
  }
}
