package com.example.iax;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IaxRegister {

    private static final String TAG = "IAX-REG";

    private static final int IAX_PORT = 4569;

    // IAX Frame Types
    private static final byte FRAME_IAX = 0x06;

    // IAX Subclasses
    private static final byte IAX_REGREQ = 0x0f;
    private static final byte IAX_REGACK = 0x10;
    private static final byte IAX_REJECT = 0x11;

    // IE Types
    private static final byte IE_USERNAME = 0x06;

    private final String host;
    private final String username;

    private DatagramSocket socket;
    private InetAddress address;

    private short srcCall = 1;

    public IaxRegister(String host, String username) {
        this.host = host;
        this.username = username;
    }

    public void start() {
        new Thread(this::run).start();
    }

    private void run() {
        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName(host);

            sendRegReq();
            receiveLoop();

        } catch (Exception e) {
            Log.e(TAG, "Registration error", e);
        }
    }

    private void sendRegReq() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(64);
        buf.order(ByteOrder.BIG_ENDIAN);

        // Full Frame Header (12 bytes)
        buf.putShort((short) (0x8000 | srcCall)); // Source call number
        buf.putShort((short) 0);                  // Destination call number
        buf.putInt(0);                            // Timestamp
        buf.put((byte) 0);                        // OSeq
        buf.put((byte) 0);                        // ISeq
        buf.put(FRAME_IAX);                       // Frame type
        buf.put(IAX_REGREQ);                      // Subclass

        // IE: USERNAME
        buf.put(IE_USERNAME);
        buf.put((byte) username.length());
        buf.put(username.getBytes());

        DatagramPacket packet = new DatagramPacket(
                buf.array(), buf.position(), address, IAX_PORT
        );
        socket.send(packet);

        Log.d(TAG, "REGREQ sent");
    }

    private void receiveLoop() throws Exception {
        byte[] buffer = new byte[256];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            ByteBuffer buf = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
            buf.order(ByteOrder.BIG_ENDIAN);

            buf.getShort(); // src
            buf.getShort(); // dst
            buf.getInt();   // ts
            buf.get();      // oseq
            buf.get();      // iseq
            buf.get();      // type
            byte subclass = buf.get();

            if (subclass == IAX_REGACK) {
                Log.d(TAG, "✅ REGISTRATION ACCEPTED");
                return;
            }

            if (subclass == IAX_REJECT) {
                Log.e(TAG, "❌ REGISTRATION REJECTED");
                return;
            }

            Log.d(TAG, "Received IAX subclass: " + subclass);
        }
    }
}
