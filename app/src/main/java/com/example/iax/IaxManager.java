package com.example.iax;

import android.content.Context;
import android.util.Log;

public class IaxManager {

    private static final String TAG = "IAX";
    private final IaxAudioHandler audioHandler;

    public interface IncomingCallListener {
        void onCallReceived(String from);
    }

    private IncomingCallListener listener;

    public IaxManager(Context context) {
        audioHandler = new IaxAudioHandler(context);
    }

    public void connect() {
        Log.d(TAG, "IAX manager initialized");
    }

    public void startCall(String extension) {
        Log.d(TAG, "Starting call to " + extension);
        audioHandler.startLoopback();
    }


    public void endCall() {
        Log.d(TAG, "Ending call");
        audioHandler.stop();
    }

    public void setIncomingCallListener(IncomingCallListener l) {
        listener = l;
    }
}
