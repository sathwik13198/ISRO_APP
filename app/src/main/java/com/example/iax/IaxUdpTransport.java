package com.example.iax;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class IaxUdpTransport {

    private static final String TAG = "IAX-UDP";
    private static final int IAX_PORT = 4569;

    private DatagramSocket socket;
    private InetAddress remoteAddr;
    private boolean running = false;

    public void start(String host) {
        try {
            socket = new DatagramSocket();
            remoteAddr = InetAddress.getByName(host);
            running = true;

            new Thread(this::receiveLoop).start();

            Log.d(TAG, "UDP socket started to " + host);
        } catch (Exception e) {
            Log.e(TAG, "UDP start failed", e);
        }
    }

    public void stop() {
        running = false;
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    public void send(byte[] data) {
        try {
            DatagramPacket packet =
                    new DatagramPacket(data, data.length, remoteAddr, IAX_PORT);
            socket.send(packet);
        } catch (Exception e) {
            Log.e(TAG, "UDP send failed", e);
        }
    }

    private void receiveLoop() {
        byte[] buffer = new byte[1024];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                Log.d(TAG, "Received UDP bytes: " + packet.getLength());
            } catch (Exception e) {
                if (running) Log.e(TAG, "UDP receive error", e);
            }
        }
    }
}
