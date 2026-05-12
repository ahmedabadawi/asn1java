package io.github.ahmedabadawi.asn1java.runtime.uper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(out.toByteArray()).isEqualTo(new byte[]{0x01, 0x00});
    }

    @Test
    void encode_one_producesTwoBytes() {
        // length=1 (0x01), value=0x01
        var out = new UperOutputStream();
        UperCodecSupport.encodeSemiConstrainedInt(out, 1);
        assertThat(out.toByteArray()).isEqualTo(new byte[]{0x01, 0x01});
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
        assertThat(out.toByteArray()).isEqualTo(new byte[]{0x00});
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
}
