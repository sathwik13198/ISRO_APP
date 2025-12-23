package com.example.isro_app.call

import android.content.Context
import android.util.Log

object SipManager {

    private const val TAG = "SIP"

    init {
        try {
            System.loadLibrary("baresip")
            System.loadLibrary("re")
            System.loadLibrary("rem")
            Log.d(TAG, "Native SIP libraries loaded")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load SIP libs", e)
        }
    }

    fun init(context: Context, myId: String) {
        Log.d(TAG, "SIP init called for $myId")
        // Native init will be added next
    }

    fun call(peerId: String) {
        Log.d(TAG, "SIP call -> $peerId")
    }

    fun answer() {
        Log.d(TAG, "SIP answer")
    }

    fun hangup() {
        Log.d(TAG, "SIP hangup")
    }
}
