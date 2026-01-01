package com.example.iax;

import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * IAX2 Frame Structure (Simplified - No Registration)
 * Handles NEW, ACCEPT, HANGUP frames and media frames
 */
public class IaxFrame {
    private static final String TAG = "IAX-FRAME";

    // Frame types (simplified - only what we need)
    public static final int FT_CONTROL = 0x06;
    public static final int FT_IAX = 0x01;
    public static final int FT_VOICE = 0x02;
    public static final int FT_VIDEO = 0x03;
    public static final int FT_TEXT = 0x04;
    public static final int FT_HTML = 0x05;
    public static final int FT_CNG = 0x07;
    public static final int FT_RTCP = 0x08;

    // IAX subclass types (control frames)
    public static final int IAX_SUBCLASS_NEW = 0x01;
    public static final int IAX_SUBCLASS_ACCEPT = 0x02;
    public static final int IAX_SUBCLASS_HANGUP = 0x04;
    public static final int IAX_SUBCLASS_REJECT = 0x05;
    public static final int IAX_SUBCLASS_ACK = 0x06;
    public static final int IAX_SUBCLASS_INVAL = 0x0a;
    public static final int IAX_SUBCLASS_LAGRQ = 0x0e;
    public static final int IAX_SUBCLASS_LAGRP = 0x0f;
    public static final int IAX_SUBCLASS_REGREQ = 0x01;
    public static final int IAX_SUBCLASS_REGACK = 0x02;
    public static final int IAX_SUBCLASS_REGAUTH = 0x03;
    public static final int IAX_SUBCLASS_REGREJ = 0x04;

    // Full frame header (12 bytes)
    public static final int FULL_FRAME_HEADER_SIZE = 12;
    // Mini frame header (4 bytes) - for media frames
    public static final int MINI_FRAME_HEADER_SIZE = 4;

    private int sourceCallNumber;
    private int destinationCallNumber;
    private long timestamp;
    private int oseqno;  // Outgoing sequence number
    private int iseqno;  // Incoming sequence number
    private int frameType;
    private int subclass;
    private byte[] data;
    private boolean isFullFrame;

    public IaxFrame() {
        this.timestamp = System.currentTimeMillis() / 1000; // Unix timestamp
        this.oseqno = 0;
        this.iseqno = 0;
        this.isFullFrame = true;
    }

    /**
     * Create a NEW frame for call initiation
     */
    public static IaxFrame createNewFrame(int sourceCallNumber, String calledNumber) {
        IaxFrame frame = new IaxFrame();
        frame.sourceCallNumber = sourceCallNumber;
        frame.destinationCallNumber = 0; // Will be set by server
        frame.frameType = FT_IAX;
        frame.subclass = IAX_SUBCLASS_NEW;
        frame.isFullFrame = true;

        // Create IAX IE data for NEW frame
        // Format: Called Number IE (0x01) + length + value
        byte[] calledBytes = calledNumber.getBytes();
        frame.data = new byte[2 + calledBytes.length];
        frame.data[0] = 0x01; // IE: Called Number
        frame.data[1] = (byte) calledBytes.length;
        System.arraycopy(calledBytes, 0, frame.data, 2, calledBytes.length);

        return frame;
    }

    /**
     * Create an ACCEPT frame for call acceptance
     */
    public static IaxFrame createAcceptFrame(int sourceCallNumber, int destinationCallNumber) {
        IaxFrame frame = new IaxFrame();
        frame.sourceCallNumber = sourceCallNumber;
        frame.destinationCallNumber = destinationCallNumber;
        frame.frameType = FT_IAX;
        frame.subclass = IAX_SUBCLASS_ACCEPT;
        frame.isFullFrame = true;
        frame.data = new byte[0]; // ACCEPT has no data

        return frame;
    }

    /**
     * Create a HANGUP frame for call termination
     */
    public static IaxFrame createHangupFrame(int sourceCallNumber, int destinationCallNumber) {
        IaxFrame frame = new IaxFrame();
        frame.sourceCallNumber = sourceCallNumber;
        frame.destinationCallNumber = destinationCallNumber;
        frame.frameType = FT_IAX;
        frame.subclass = IAX_SUBCLASS_HANGUP;
        frame.isFullFrame = true;
        frame.data = new byte[0]; // HANGUP has no data

        return frame;
    }

    /**
     * Create a voice/media frame
     */
    public static IaxFrame createVoiceFrame(int sourceCallNumber, int destinationCallNumber, byte[] audioData) {
        IaxFrame frame = new IaxFrame();
        frame.sourceCallNumber = sourceCallNumber;
        frame.destinationCallNumber = destinationCallNumber;
        frame.frameType = FT_VOICE;
        frame.subclass = 0x00; // ulaw codec
        frame.isFullFrame = false; // Use mini frame for voice
        frame.data = audioData;

        return frame;
    }

    /**
     * Encode frame to byte array for transmission
     */
    public byte[] encode() {
        if (isFullFrame) {
            return encodeFullFrame();
        } else {
            return encodeMiniFrame();
        }
    }

    /**
     * Encode full frame (12-byte header + data)
     */
    private byte[] encodeFullFrame() {
        int dataLength = (data != null) ? data.length : 0;
        byte[] frame = new byte[FULL_FRAME_HEADER_SIZE + dataLength];

        ByteBuffer buffer = ByteBuffer.wrap(frame);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Source call number (2 bytes, big-endian)
        buffer.putShort((short) (sourceCallNumber & 0x7FFF));

        // Destination call number (2 bytes, big-endian)
        buffer.putShort((short) (destinationCallNumber & 0x7FFF));

        // Timestamp (4 bytes, big-endian)
        buffer.putInt((int) (timestamp & 0xFFFFFFFF));

        // OSeqno and ISeqno (1 byte each)
        buffer.put((byte) (oseqno & 0xFF));
        buffer.put((byte) (iseqno & 0xFF));

        // Frame type (1 byte) - upper 2 bits are flags
        int frameTypeByte = (frameType & 0x0F) << 4;
        if (dataLength > 0) {
            frameTypeByte |= 0x08; // Set data bit
        }
        buffer.put((byte) frameTypeByte);

        // Subclass (1 byte)
        buffer.put((byte) (subclass & 0xFF));

        // Data (if present)
        if (data != null && data.length > 0) {
            buffer.put(data);
        }

        return frame;
    }

    /**
     * Encode mini frame (4-byte header + data) - for voice frames
     */
    private byte[] encodeMiniFrame() {
        int dataLength = (data != null) ? data.length : 0;
        byte[] frame = new byte[MINI_FRAME_HEADER_SIZE + dataLength];

        ByteBuffer buffer = ByteBuffer.wrap(frame);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Source call number (2 bytes, big-endian) with F bit set
        buffer.putShort((short) ((sourceCallNumber & 0x7FFF) | 0x8000));

        // Timestamp (2 bytes, big-endian) - truncated
        buffer.putShort((short) ((timestamp & 0xFFFF)));

        // Data
        if (data != null && data.length > 0) {
            buffer.put(data);
        }

        return frame;
    }

    /**
     * Decode byte array to IAX frame
     */
    public static IaxFrame decode(byte[] frameData) {
        if (frameData.length < MINI_FRAME_HEADER_SIZE) {
            Log.e(TAG, "Frame too short: " + frameData.length);
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(frameData);
        buffer.order(ByteOrder.BIG_ENDIAN);

        IaxFrame frame = new IaxFrame();

        // Check if it's a full frame or mini frame
        short firstWord = buffer.getShort();
        boolean isFullFrame = (firstWord & 0x8000) == 0;

        buffer.rewind();

        if (isFullFrame && frameData.length >= FULL_FRAME_HEADER_SIZE) {
            // Full frame
            frame.sourceCallNumber = buffer.getShort() & 0x7FFF;
            frame.destinationCallNumber = buffer.getShort() & 0x7FFF;
            frame.timestamp = buffer.getInt() & 0xFFFFFFFFL;
            frame.oseqno = buffer.get() & 0xFF;
            frame.iseqno = buffer.get() & 0xFF;

            int frameTypeByte = buffer.get() & 0xFF;
            frame.frameType = (frameTypeByte >> 4) & 0x0F;
            frame.subclass = buffer.get() & 0xFF;

            int dataLength = frameData.length - FULL_FRAME_HEADER_SIZE;
            if (dataLength > 0) {
                frame.data = new byte[dataLength];
                buffer.get(frame.data);
            }

            frame.isFullFrame = true;
        } else {
            // Mini frame (voice)
            frame.sourceCallNumber = buffer.getShort() & 0x7FFF;
            frame.timestamp = buffer.getShort() & 0xFFFF;
            frame.frameType = FT_VOICE;
            frame.subclass = 0x00; // ulaw

            int dataLength = frameData.length - MINI_FRAME_HEADER_SIZE;
            if (dataLength > 0) {
                frame.data = new byte[dataLength];
                buffer.get(frame.data);
            }

            frame.isFullFrame = false;
        }

        return frame;
    }

    // Getters and setters
    public int getSourceCallNumber() {
        return sourceCallNumber;
    }

    public void setSourceCallNumber(int sourceCallNumber) {
        this.sourceCallNumber = sourceCallNumber;
    }

    public int getDestinationCallNumber() {
        return destinationCallNumber;
    }

    public void setDestinationCallNumber(int destinationCallNumber) {
        this.destinationCallNumber = destinationCallNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getOseqno() {
        return oseqno;
    }

    public void setOseqno(int oseqno) {
        this.oseqno = oseqno;
    }

    public int getIseqno() {
        return iseqno;
    }

    public void setIseqno(int iseqno) {
        this.iseqno = iseqno;
    }

    public int getFrameType() {
        return frameType;
    }

    public void setFrameType(int frameType) {
        this.frameType = frameType;
    }

    public int getSubclass() {
        return subclass;
    }

    public void setSubclass(int subclass) {
        this.subclass = subclass;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public boolean isFullFrame() {
        return isFullFrame;
    }

    public void setFullFrame(boolean fullFrame) {
        isFullFrame = fullFrame;
    }
}

