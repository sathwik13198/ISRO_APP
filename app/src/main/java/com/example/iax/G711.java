package com.example.iax;

public class G711 {

    private static final int BIAS = 0x84;
    private static final int CLIP = 32635;

    public static byte linearToMuLaw(short sample) {
        int sign;
        int exponent;
        int mantissa;
        int muLawByte;

        sign = (sample >> 8) & 0x80;
        if (sign != 0) sample = (short) -sample;
        if (sample > CLIP) sample = CLIP;

        sample = (short) (sample + BIAS);

        exponent = 7;
        for (int expMask = 0x4000; (sample & expMask) == 0 && exponent > 0; expMask >>= 1) {
            exponent--;
        }

        mantissa = (sample >> (exponent + 3)) & 0x0F;
        muLawByte = ~(sign | (exponent << 4) | mantissa);

        return (byte) muLawByte;
    }

    public static byte[] encode(short[] pcm) {
        byte[] ulaw = new byte[pcm.length];
        for (int i = 0; i < pcm.length; i++) {
            ulaw[i] = linearToMuLaw(pcm[i]);
        }
        return ulaw;
    }
}
