package com.example.iax;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class IaxUdpTransport {

    private static final String TAG = "IAX-UDP";
    private static final int IAX_PORT = 4569; // Standard IAX port

    private DatagramSocket socket;
    private InetAddress remoteAddr;
    private int remotePort = IAX_PORT;
    private boolean running = false;
    private IaxFrameListener frameListener;
    private int sequenceNumber = 0;

    public interface IaxFrameListener {
        void onFrameReceived(IaxFrame frame);
    }

    public void setFrameListener(IaxFrameListener listener) {
        this.frameListener = listener;
    }

    public void start(String host) {
        start(host, IAX_PORT);
    }

    public void start(String host, int port) {
        try {
            socket = new DatagramSocket();
            remoteAddr = InetAddress.getByName(host);
            remotePort = port;
            running = true;

            new Thread(this::receiveLoop).start();

            Log.d(TAG, "UDP socket started to " + host + ":" + port);
        } catch (Exception e) {
            Log.e(TAG, "UDP start failed", e);
        }
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
            socket = null;
        }
        Log.d(TAG, "UDP socket stopped");
    }

    /**
     * Send IAX frame
     */
    public void sendFrame(IaxFrame frame) {
        if (frame == null || socket == null || !running) {
            return;
        }

        try {
            // Set sequence number
            frame.setOseqno(sequenceNumber++);
            if (sequenceNumber > 255) {
                sequenceNumber = 0;
            }

            byte[] frameData = frame.encode();
            DatagramPacket packet = new DatagramPacket(
                    frameData, frameData.length, remoteAddr, remotePort);
            socket.send(packet);

            Log.d(TAG, "Sent IAX frame: type=" + frame.getFrameType() +
                    ", subclass=" + frame.getSubclass() +
                    ", size=" + frameData.length);
        } catch (Exception e) {
            Log.e(TAG, "UDP send frame failed", e);
        }
    }

    /**
     * Send raw bytes (for backward compatibility)
     */
    public void send(byte[] data) {
        if (data == null || socket == null || !running) {
            return;
        }

        try {
            DatagramPacket packet = new DatagramPacket(
                    data, data.length, remoteAddr, remotePort);
            socket.send(packet);
            Log.d(TAG, "Sent raw bytes: " + data.length);
        } catch (Exception e) {
            Log.e(TAG, "UDP send failed", e);
        }
    }

    /**
     * Send command (legacy method - kept for compatibility)
     */
    public void sendCommand(String cmd) {
        new Thread(() -> {
            try {
                byte[] data = cmd.getBytes();
                send(data);
                Log.d(TAG, "Sent command: " + cmd);
            } catch (Exception e) {
                Log.e(TAG, "Send command failed", e);
            }
        }).start();
    }

    /**
     * Receive loop for IAX frames
     */
    private void receiveLoop() {
        byte[] buffer = new byte[2048]; // Increased buffer for IAX frames

        while (running) {
            try {
                if (socket == null || socket.isClosed()) {
                    break;
                }

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Extract received data
                byte[] receivedData = new byte[packet.getLength()];
                System.arraycopy(buffer, 0, receivedData, 0, packet.getLength());

                // Decode IAX frame
                IaxFrame frame = IaxFrame.decode(receivedData);
                if (frame != null && frameListener != null) {
                    frameListener.onFrameReceived(frame);
                } else if (frame == null) {
                    Log.w(TAG, "Failed to decode IAX frame, length: " + receivedData.length);
                }

            } catch (Exception e) {
                if (running) {
                    Log.e(TAG, "UDP receive error", e);
                }
            }
        }

        Log.d(TAG, "Receive loop stopped");
    }

    public boolean isRunning() {
        return running && socket != null && !socket.isClosed();
    }

    public InetAddress getRemoteAddress() {
        return remoteAddr;
    }

    public int getRemotePort() {
        return remotePort;
    }
}
