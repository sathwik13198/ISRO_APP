package com.example.iax;

/**
 * IAX Utility Functions
 * Provides helper methods for IAX protocol operations
 */
public class IaxUtils {

    /**
     * Generate a random call number (source call number)
     * IAX uses call numbers to identify calls
     */
    public static int generateCallNumber() {
        // Generate random number between 1 and 32767 (15-bit)
        return (int) (Math.random() * 32767) + 1;
    }

    /**
     * Validate IAX frame data
     */
    public static boolean isValidFrame(byte[] frameData) {
        if (frameData == null || frameData.length < 4) {
            return false;
        }
        return true;
    }

    /**
     * Extract called number from NEW frame data
     */
    public static String extractCalledNumber(byte[] frameData) {
        if (frameData == null || frameData.length < 3) {
            return null;
        }

        // Check if it's a Called Number IE (0x01)
        if (frameData[0] == 0x01) {
            int length = frameData[1] & 0xFF;
            if (length > 0 && frameData.length >= 2 + length) {
                return new String(frameData, 2, length);
            }
        }

        return null;
    }
}
