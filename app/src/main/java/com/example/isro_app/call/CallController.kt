package com.example.isro_app.call

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.iax.IaxManager
import com.example.isro_app.mqtt.MqttManager

class CallController(
    private val context: Context,
    private val mqtt: MqttManager,
    private val iax: IaxManager
) {

    private val TAG = "CallController"
    private var currentPeerId: String? = null

    init {
        // Set up IAX call event listener
        iax.setCallEventListener(object : IaxManager.CallEventListener {
            override fun onCallAccepted(destinationCallNumber: Int) {
                Log.d(TAG, "IAX call accepted, destination: $destinationCallNumber")
                // Audio is already started by IaxManager
            }

            override fun onCallRejected() {
                Log.d(TAG, "IAX call rejected")
                currentPeerId = null
            }

            override fun onCallHangup() {
                Log.d(TAG, "IAX call hung up")
                currentPeerId = null
            }
        })
    }

    /**
     * Initiate outgoing call
     * Flow: MQTT CALL_REQUEST → Wait for CALL_ACCEPT → Start IAX call
     */
    fun outgoingCall(peerId: String) {
        Log.d(TAG, "Outgoing call to $peerId")
        currentPeerId = peerId
        // Send MQTT call request (signaling)
        mqtt.sendCallRequest(peerId)
        // Note: IAX call will start when we receive CALL_ACCEPT
    }

    /**
     * Accept incoming call
     * Flow: MQTT CALL_ACCEPT → Start IAX call → Audio starts
     */
    fun acceptCall(peerId: String) {
        Log.d(TAG, "Accepting call from $peerId")
        currentPeerId = peerId
        
        // Send MQTT accept (signaling)
        mqtt.sendCallAccept(peerId)
        
        // Configure audio routing
        routeAudio()
        
        // Start IAX call (this will start audio capture/playback)
        iax.startCall(peerId)
    }

    /**
     * Reject incoming call
     */
    fun rejectCall(peerId: String) {
        Log.d(TAG, "Rejecting call from $peerId")
        mqtt.sendCallReject(peerId)
        currentPeerId = null
    }

    /**
     * End current call
     */
    fun endCall(peerId: String) {
        Log.d(TAG, "Ending call with $peerId")
        mqtt.sendCallEnd(peerId)
        stopAudio()
        currentPeerId = null
    }

    /**
     * Called when we receive CALL_ACCEPT from peer via MQTT
     * This means the peer accepted our call request - start IAX call
     */
    fun onCallAccepted(peerId: String) {
        Log.d(TAG, "Received CALL_ACCEPT from $peerId, starting IAX call")
        currentPeerId = peerId
        
        // Configure audio routing
        routeAudio()
        
        // Start IAX call (audio will start automatically)
        iax.startCall(peerId)
    }

    /**
     * Called when we receive CALL_END from peer via MQTT
     */
    fun onCallEnded() {
        Log.d(TAG, "Received CALL_END, stopping audio")
        stopAudio()
        currentPeerId = null
    }

    /**
     * Configure audio routing for voice call
     */
    private fun routeAudio() {
        val audioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
        Log.d(TAG, "Audio routed for communication mode")
    }

    /**
     * Stop audio and hangup IAX call
     */
    private fun stopAudio() {
        iax.hangup()
        Log.d(TAG, "Audio stopped, IAX call hung up")
    }

    /**
     * Get current call state
     */
    fun getCallState(): IaxManager.CallState {
        return iax.getCallState()
    }

    /**
     * Check if in call
     */
    fun isInCall(): Boolean {
        return iax.getCallState() != IaxManager.CallState.IDLE
    }
}

