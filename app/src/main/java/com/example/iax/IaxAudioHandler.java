package com.example.iax;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class IaxAudioHandler {

    private static final String TAG = "IAX-AUDIO";
    private static final int SAMPLE_RATE = 8000;

    private final Context context;
    private AudioRecord recorder;
    private boolean running = false;

    // 🔧 STEP 7.2 — UDP transport
    private IaxUdpTransport udp;

    public IaxAudioHandler(Context context) {
        this.context = context;

        // 🔴 REPLACE with your Asterisk IP
        udp = new IaxUdpTransport();
        udp.start("192.168.29.242");
    }

    @SuppressLint("MissingPermission")
    public void startLoopback() {

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted");
            return;
        }

        int recBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recBuffer
        );

        recorder.startRecording();
        running = true;

        new Thread(() -> {
            byte[] pcmBytes = new byte[recBuffer];

            while (running) {
                int read = recorder.read(pcmBytes, 0, pcmBytes.length);

                if (read > 0) {
                    short[] pcmShorts = new short[read / 2];

                    for (int i = 0; i < pcmShorts.length; i++) {
                        pcmShorts[i] = (short) (
                                (pcmBytes[i * 2] & 0xff) |
                                (pcmBytes[i * 2 + 1] << 8)
                        );
                    }

                    // 🎙️ Encode PCM → G711
                    byte[] g711 = G711.encode(pcmShorts);

                    // 🚀 SEND OVER UDP (IAX MEDIA)
                    udp.send(g711);

                    Log.d(TAG, "Encoded & sent G711 bytes: " + g711.length);
                }
            }
        }, "IAX-Audio-Thread").start();

        Log.d(TAG, "Audio capture + UDP streaming started");
    }

    public void stop() {
        running = false;

        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }

        if (udp != null) {
            udp.stop();
        }

        Log.d(TAG, "Audio capture and UDP stopped");
    }
}
