package com.example.iax;

/**
 * G.711 μ-law (ulaw) Codec Implementation
 * Converts between PCM audio and μ-law encoded audio
 */
public class IaxCodec {

    private static final int BIAS = 0x84;
    private static final int CLIP = 32635;
    private static final int SIGN_BIT = 0x80;
    private static final int QUANT_MASK = 0x0F;
    private static final int SEG_SHIFT = 4;
    private static final int SEG_MASK = 0x70;

    // μ-law to linear conversion table
    private static final short[] MULAW_TO_LIN = {
        -32124, -31100, -30076, -29052, -28028, -27004, -25980, -24956,
        -23932, -22908, -21884, -20860, -19836, -18812, -17788, -16764,
        -15996, -15484, -14972, -14460, -13948, -13436, -12924, -12412,
        -11900, -11388, -10876, -10364, -9852, -9340, -8828, -8316,
        -7932, -7676, -7420, -7164, -6908, -6652, -6396, -6140,
        -5884, -5628, -5372, -5116, -4860, -4604, -4348, -4092,
        -3900, -3772, -3644, -3516, -3388, -3260, -3132, -3004,
        -2876, -2748, -2620, -2492, -2364, -2236, -2108, -1980,
        -1884, -1820, -1756, -1692, -1628, -1564, -1500, -1436,
        -1372, -1308, -1244, -1180, -1116, -1052, -988, -924,
        -876, -844, -812, -780, -748, -716, -684, -652,
        -620, -588, -556, -524, -492, -460, -428, -396,
        -372, -356, -340, -324, -308, -292, -276, -260,
        -244, -228, -212, -196, -180, -164, -148, -132,
        -120, -112, -104, -96, -88, -80, -72, -64,
        -56, -48, -40, -32, -24, -16, -8, 0,
        32124, 31100, 30076, 29052, 28028, 27004, 25980, 24956,
        23932, 22908, 21884, 20860, 19836, 18812, 17788, 16764,
        15996, 15484, 14972, 14460, 13948, 13436, 12924, 12412,
        11900, 11388, 10876, 10364, 9852, 9340, 8828, 8316,
        7932, 7676, 7420, 7164, 6908, 6652, 6396, 6140,
        5884, 5628, 5372, 5116, 4860, 4604, 4348, 4092,
        3900, 3772, 3644, 3516, 3388, 3260, 3132, 3004,
        2876, 2748, 2620, 2492, 2364, 2236, 2108, 1980,
        1884, 1820, 1756, 1692, 1628, 1564, 1500, 1436,
        1372, 1308, 1244, 1180, 1116, 1052, 988, 924,
        876, 844, 812, 780, 748, 716, 684, 652,
        620, 588, 556, 524, 492, 460, 428, 396,
        372, 356, 340, 324, 308, 292, 276, 260,
        244, 228, 212, 196, 180, 164, 148, 132,
        120, 112, 104, 96, 88, 80, 72, 64,
        56, 48, 40, 32, 24, 16, 8, 0
    };

    /**
     * Encode PCM 16-bit linear audio to μ-law
     * @param pcmData 16-bit PCM audio data (little-endian)
     * @return μ-law encoded audio data
     */
    public static byte[] encodeUlaw(byte[] pcmData) {
        if (pcmData == null || pcmData.length == 0) {
            return new byte[0];
        }

        // PCM is 16-bit, so we process 2 bytes at a time
        int samples = pcmData.length / 2;
        byte[] ulawData = new byte[samples];

        for (int i = 0; i < samples; i++) {
            // Read 16-bit sample (little-endian)
            int sample = (pcmData[i * 2] & 0xFF) | ((pcmData[i * 2 + 1] & 0xFF) << 8);
            // Convert to signed 16-bit
            if (sample > 32767) {
                sample -= 65536;
            }

            ulawData[i] = linearToUlaw(sample);
        }

        return ulawData;
    }

    /**
     * Decode μ-law audio to PCM 16-bit linear
     * @param ulawData μ-law encoded audio data
     * @return 16-bit PCM audio data (little-endian)
     */
    public static byte[] decodeUlaw(byte[] ulawData) {
        if (ulawData == null || ulawData.length == 0) {
            return new byte[0];
        }

        byte[] pcmData = new byte[ulawData.length * 2];

        for (int i = 0; i < ulawData.length; i++) {
            short sample = ulawToLinear(ulawData[i]);
            // Write as little-endian 16-bit
            pcmData[i * 2] = (byte) (sample & 0xFF);
            pcmData[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }

        return pcmData;
    }

    /**
     * Convert linear PCM sample to μ-law
     */
    private static byte linearToUlaw(int sample) {
        int sign = (sample >> 8) & SIGN_BIT;
        if (sample < 0) {
            sample = -sample;
            sign = SIGN_BIT;
        } else {
            sign = 0;
        }

        if (sample > CLIP) {
            sample = CLIP;
        }

        sample += BIAS;
        int exponent = expLut[(sample >> 7) & 0xFF];
        int mantissa = (sample >> (exponent + 3)) & QUANT_MASK;
        int ulawbyte = ~(sign | (exponent << SEG_SHIFT) | mantissa);

        return (byte) ulawbyte;
    }

    /**
     * Convert μ-law sample to linear PCM
     */
    private static short ulawToLinear(byte ulawbyte) {
        ulawbyte = (byte) ~ulawbyte;
        int sign = ulawbyte & SIGN_BIT;
        int exponent = (ulawbyte & SEG_MASK) >> SEG_SHIFT;
        int mantissa = ulawbyte & QUANT_MASK;

        int sample = mantissa << (exponent + 3);
        sample += BIAS << exponent;
        if (sign != 0) {
            sample = -sample;
        }

        return (short) sample;
    }

    // Exponent lookup table for μ-law encoding
    private static final int[] expLut = {
        0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
        5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
        5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
        7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7
    };
}

