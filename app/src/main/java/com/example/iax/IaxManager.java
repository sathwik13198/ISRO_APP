package com.example.iax;

import android.util.Log;

public class IaxManager implements IaxUdpTransport.IaxFrameListener, IaxAudioHandler.IaxAudioCallback {

    private static final String TAG = "IAX-MANAGER";

    private final IaxUdpTransport udp;
    private final String asteriskIp;
    private IaxAudioHandler audioHandler;
    private int sourceCallNumber;
    private int destinationCallNumber = 0;
    private CallState callState = CallState.IDLE;
    private String currentPeerId = null;
    private CallEventListener callEventListener;

    public enum CallState {
        IDLE,
        CALLING,
        RINGING,
        ACTIVE,
        HANGING_UP
    }

    public interface CallEventListener {
        void onCallAccepted(int destinationCallNumber);
        void onCallRejected();
        void onCallHangup();
    }

    public IaxManager(String asteriskIp) {
        this.asteriskIp = asteriskIp;
        this.udp = new IaxUdpTransport();
        this.udp.setFrameListener(this);
        this.sourceCallNumber = IaxUtils.generateCallNumber();
        this.audioHandler = new IaxAudioHandler(this);
    }

    public void setCallEventListener(CallEventListener listener) {
        this.callEventListener = listener;
    }

    public void connect() {
        udp.start(asteriskIp);
        Log.d(TAG, "Connected to Asterisk server at " + asteriskIp);
    }

    /**
     * Start a call to the specified peer (no registration needed)
     */
    public void startCall(String peerId) {
        if (callState != CallState.IDLE) {
            Log.w(TAG, "Call already in progress, state: " + callState);
            return;
        }

        Log.d(TAG, "Starting call to " + peerId);
        currentPeerId = peerId;
        callState = CallState.CALLING;

        // Create NEW frame
        IaxFrame newFrame = IaxFrame.createNewFrame(sourceCallNumber, peerId);
        udp.sendFrame(newFrame);

        Log.d(TAG, "NEW frame sent to " + peerId);
    }

    /**
     * Accept incoming call
     */
    public void acceptCall(int destCallNumber) {
        if (callState != CallState.RINGING) {
            Log.w(TAG, "Cannot accept call, state: " + callState);
            return;
        }

        Log.d(TAG, "Accepting call, destination call number: " + destCallNumber);
        this.destinationCallNumber = destCallNumber;
        callState = CallState.ACTIVE;

        // Send ACCEPT frame
        IaxFrame acceptFrame = IaxFrame.createAcceptFrame(sourceCallNumber, destinationCallNumber);
        udp.sendFrame(acceptFrame);

        // Start audio
        startAudio();

        Log.d(TAG, "Call accepted, audio started");
    }

    /**
     * Hangup current call
     */
    public void hangup() {
        if (callState == CallState.IDLE) {
            Log.w(TAG, "No call to hangup");
            return;
        }

        Log.d(TAG, "Hanging up call");
        callState = CallState.HANGING_UP;

        // Stop audio
        stopAudio();

        // Send HANGUP frame if we have a destination call number
        if (destinationCallNumber > 0) {
            IaxFrame hangupFrame = IaxFrame.createHangupFrame(sourceCallNumber, destinationCallNumber);
            udp.sendFrame(hangupFrame);
        }

        // Reset state
        resetCallState();

        Log.d(TAG, "Call hung up");
    }

    /**
     * Handle incoming IAX frames
     */
    @Override
    public void onFrameReceived(IaxFrame frame) {
        if (frame == null) {
            return;
        }

        Log.d(TAG, "Frame received: type=" + frame.getFrameType() +
                ", subclass=" + frame.getSubclass() +
                ", destCall=" + frame.getDestinationCallNumber());

        // Handle different frame types
        if (frame.getFrameType() == IaxFrame.FT_IAX) {
            handleControlFrame(frame);
        } else if (frame.getFrameType() == IaxFrame.FT_VOICE) {
            handleVoiceFrame(frame);
        }
    }

    /**
     * Handle control frames (NEW, ACCEPT, HANGUP)
     */
    private void handleControlFrame(IaxFrame frame) {
        int subclass = frame.getSubclass();

        switch (subclass) {
            case IaxFrame.IAX_SUBCLASS_NEW:
                handleNewFrame(frame);
                break;

            case IaxFrame.IAX_SUBCLASS_ACCEPT:
                handleAcceptFrame(frame);
                break;

            case IaxFrame.IAX_SUBCLASS_HANGUP:
                handleHangupFrame(frame);
                break;

            case IaxFrame.IAX_SUBCLASS_REJECT:
                handleRejectFrame(frame);
                break;

            default:
                Log.d(TAG, "Unhandled control frame subclass: " + subclass);
        }
    }

    /**
     * Handle NEW frame (incoming call)
     */
    private void handleNewFrame(IaxFrame frame) {
        Log.d(TAG, "Received NEW frame");
        destinationCallNumber = frame.getSourceCallNumber();

        // Extract called number from frame data
        String calledNumber = IaxUtils.extractCalledNumber(frame.getData());
        if (calledNumber != null) {
            currentPeerId = calledNumber;
        }

        callState = CallState.RINGING;

        // Notify listener (if set) - Android app will handle MQTT signaling
        // For now, auto-accept for simplicity (can be changed later)
        Log.d(TAG, "Incoming call from " + calledNumber);
    }

    /**
     * Handle ACCEPT frame (call accepted)
     */
    private void handleAcceptFrame(IaxFrame frame) {
        Log.d(TAG, "Received ACCEPT frame");
        if (callState == CallState.CALLING) {
            destinationCallNumber = frame.getSourceCallNumber();
            callState = CallState.ACTIVE;

            // Start audio
            startAudio();

            if (callEventListener != null) {
                callEventListener.onCallAccepted(destinationCallNumber);
            }

            Log.d(TAG, "Call accepted, audio started");
        }
    }

    /**
     * Handle HANGUP frame
     */
    private void handleHangupFrame(IaxFrame frame) {
        Log.d(TAG, "Received HANGUP frame");
        stopAudio();
        resetCallState();

        if (callEventListener != null) {
            callEventListener.onCallHangup();
        }
    }

    /**
     * Handle REJECT frame
     */
    private void handleRejectFrame(IaxFrame frame) {
        Log.d(TAG, "Received REJECT frame");
        resetCallState();

        if (callEventListener != null) {
            callEventListener.onCallRejected();
        }
    }

    /**
     * Handle voice/media frames
     */
    private void handleVoiceFrame(IaxFrame frame) {
        if (callState == CallState.ACTIVE && frame.getData() != null) {
            // Write audio data to playback
            audioHandler.writeAudioData(frame.getData());
        }
    }

    /**
     * Start audio capture and playback
     */
    private void startAudio() {
        if (!audioHandler.isRecording()) {
            audioHandler.startCapture();
        }
        if (!audioHandler.isPlaying()) {
            audioHandler.startPlayback();
        }
        Log.d(TAG, "Audio started");
    }

    /**
     * Stop audio capture and playback
     */
    private void stopAudio() {
        audioHandler.stopAll();
        Log.d(TAG, "Audio stopped");
    }

    /**
     * Handle captured audio data (from IaxAudioHandler callback)
     */
    @Override
    public void onAudioDataCaptured(byte[] audioData) {
        if (callState == CallState.ACTIVE && destinationCallNumber > 0) {
            // Create voice frame and send
            IaxFrame voiceFrame = IaxFrame.createVoiceFrame(
                    sourceCallNumber, destinationCallNumber, audioData);
            udp.sendFrame(voiceFrame);
        }
    }

    /**
     * Reset call state
     */
    private void resetCallState() {
        callState = CallState.IDLE;
        destinationCallNumber = 0;
        currentPeerId = null;
    }

    /**
     * Disconnect from Asterisk
     */
    public void disconnect() {
        hangup();
        udp.stop();
        audioHandler.stopAll();
        Log.d(TAG, "Disconnected from Asterisk");
    }

    public CallState getCallState() {
        return callState;
    }

    public int getSourceCallNumber() {
        return sourceCallNumber;
    }

    public int getDestinationCallNumber() {
        return destinationCallNumber;
    }
}
