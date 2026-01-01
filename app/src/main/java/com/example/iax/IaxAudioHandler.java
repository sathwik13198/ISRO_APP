package com.example.iax;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Handles audio capture and playback for IAX calls
 * Uses AudioRecord for capture and AudioTrack for playback
 */
public class IaxAudioHandler {

    private static final String TAG = "IAX-AUDIO";

    // Audio configuration for G.711 μ-law
    private static final int SAMPLE_RATE = 8000; // 8 kHz for G.711
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_FACTOR = 2; // Buffer size multiplier

    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private Thread captureThread;
    private Thread playbackThread;
    private IaxAudioCallback callback;
    private int bufferSize;

    public interface IaxAudioCallback {
        void onAudioDataCaptured(byte[] audioData);
    }

    public IaxAudioHandler(IaxAudioCallback callback) {
        this.callback = callback;
        initializeAudio();
    }

    /**
     * Initialize audio buffers
     */
    private void initializeAudio() {
        // Calculate buffer size
        bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;

        if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
            Log.e(TAG, "Invalid buffer size");
            bufferSize = 1600; // Fallback: 1600 bytes = 100ms at 8kHz, 16-bit, mono
        }

        Log.d(TAG, "Audio buffer size: " + bufferSize);
    }

    /**
     * Start audio capture
     */
    public void startCapture() {
        if (isRecording) {
            Log.w(TAG, "Already recording");
            return;
        }

        try {
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                return;
            }

            isRecording = true;
            audioRecord.startRecording();

            captureThread = new Thread(this::captureLoop);
            captureThread.start();

            Log.d(TAG, "Audio capture started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start audio capture", e);
            isRecording = false;
        }
    }

    /**
     * Stop audio capture
     */
    public void stopCapture() {
        if (!isRecording) {
            return;
        }

        isRecording = false;

        if (captureThread != null) {
            try {
                captureThread.join(1000);
            } catch (InterruptedException e) {
                Log.w(TAG, "Capture thread join interrupted", e);
            }
            captureThread = null;
        }

        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioRecord", e);
            }
            audioRecord = null;
        }

        Log.d(TAG, "Audio capture stopped");
    }

    /**
     * Start audio playback
     */
    public void startPlayback() {
        if (isPlaying) {
            Log.w(TAG, "Already playing");
            return;
        }

        try {
            audioTrack = new AudioTrack(
                    android.media.AudioManager.STREAM_VOICE_CALL,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AUDIO_FORMAT,
                    bufferSize,
                    AudioTrack.MODE_STREAM);

            if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "AudioTrack initialization failed");
                return;
            }

            isPlaying = true;
            audioTrack.play();

            playbackThread = new Thread(this::playbackLoop);
            playbackThread.start();

            Log.d(TAG, "Audio playback started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start audio playback", e);
            isPlaying = false;
        }
    }

    /**
     * Stop audio playback
     */
    public void stopPlayback() {
        if (!isPlaying) {
            return;
        }

        isPlaying = false;

        if (playbackThread != null) {
            try {
                playbackThread.join(1000);
            } catch (InterruptedException e) {
                Log.w(TAG, "Playback thread join interrupted", e);
            }
            playbackThread = null;
        }

        if (audioTrack != null) {
            try {
                if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.stop();
                }
                audioTrack.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioTrack", e);
            }
            audioTrack = null;
        }

        Log.d(TAG, "Audio playback stopped");
    }

    /**
     * Write audio data for playback (μ-law encoded)
     */
    public void writeAudioData(byte[] ulawData) {
        if (!isPlaying || audioTrack == null || ulawData == null) {
            return;
        }

        try {
            // Decode μ-law to PCM
            byte[] pcmData = IaxCodec.decodeUlaw(ulawData);

            // Write to AudioTrack
            int written = audioTrack.write(pcmData, 0, pcmData.length);
            if (written < 0) {
                Log.w(TAG, "AudioTrack write error: " + written);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error writing audio data", e);
        }
    }

    /**
     * Capture loop - reads audio and encodes to μ-law
     */
    private void captureLoop() {
        byte[] buffer = new byte[bufferSize];

        while (isRecording && audioRecord != null) {
            try {
                int bytesRead = audioRecord.read(buffer, 0, buffer.length);

                if (bytesRead > 0 && callback != null) {
                    // Encode PCM to μ-law
                    byte[] ulawData = IaxCodec.encodeUlaw(buffer);
                    callback.onAudioDataCaptured(ulawData);
                } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "AudioRecord read error: INVALID_OPERATION");
                    break;
                } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "AudioRecord read error: BAD_VALUE");
                    break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in capture loop", e);
                break;
            }
        }

        Log.d(TAG, "Capture loop ended");
    }

    /**
     * Playback loop - simplified, uses writeAudioData for actual playback
     */
    private void playbackLoop() {
        // Playback is handled by writeAudioData method
        // This thread just keeps the loop running
        while (isPlaying) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }

        Log.d(TAG, "Playback loop ended");
    }

    /**
     * Stop all audio (capture and playback)
     */
    public void stopAll() {
        stopCapture();
        stopPlayback();
    }

    /**
     * Check if recording
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Check if playing
     */
    public boolean isPlaying() {
        return isPlaying;
    }
}

