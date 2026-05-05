package io.github.ahmedabadawi.asn1java.handwritten.uper;

public class UperInputStream {

    private final byte[] data;
    private int bitPosition = 0;

    public UperInputStream(byte[] data) {
        this.data = data;
    }

    public long readBits(int numBits) {
        long result = 0;
        for (int i = 0; i < numBits; i++) {
            int byteIndex = bitPosition / 8;
            int bitIndex = 7 - (bitPosition % 8);
            int bit = (data[byteIndex] >>> bitIndex) & 1;
            result = (result << 1) | bit;
            bitPosition++;
        }
        return result;
    }
}
