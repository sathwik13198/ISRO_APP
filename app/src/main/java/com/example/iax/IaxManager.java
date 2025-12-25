package com.example.iax;

import android.util.Log;

public class IaxManager {

    private static final String TAG = "IAX-MANAGER";

    private final IaxUdpTransport udp;
    private final String asteriskIp;

    public IaxManager(String asteriskIp) {
        this.asteriskIp = asteriskIp;
        this.udp = new IaxUdpTransport();
    }

    public void connect() {
        udp.start(asteriskIp);
        Log.d(TAG, "Connected to Asterisk UDP controller");
    }

    public void call(String extension) {
        String msg = "CALL " + extension;
        udp.send(msg.getBytes());
        Log.d(TAG, "CALL sent: " + extension);
    }

    public void startCall(String extension) {
        Log.d("IAX-MANAGER", "Starting call to " + extension);

        new Thread(() -> {
            udp.sendCommand("CALL " + extension);
        }).start();
    }

    public void hangup() {
        String msg = "HANGUP";
        udp.send(msg.getBytes());
        Log.d(TAG, "HANGUP sent");
    }

    public void disconnect() {
        udp.stop();
        Log.d(TAG, "Disconnected");
    }
}
