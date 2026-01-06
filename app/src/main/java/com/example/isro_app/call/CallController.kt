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
    private var incomingCallNumber: Int = 0  // Store incoming call number
    
    // Listener for UI to know about incoming IAX calls
    var onIncomingIaxCall: ((String, Int) -> Unit)? = null

    init {
        // Set up IAX call event listener
        iax.setCallEventListener(object : IaxManager.CallEventListener {
            override fun onCallAccepted(destinationCallNumber: Int) {
                Log.d(TAG, "IAX call accepted, destination: $destinationCallNumber")
                // Audio is already started by IaxManager
            }

            override fun onCallRejected() {
                Log.d(TAG, "IAX call rejected")
                resetCall()
            }

            override fun onCallHangup() {
                Log.d(TAG, "IAX call hung up")
                resetCall()
            }
            
            // ✅ NEW: Handle incoming IAX calls
            override fun onIncomingCall(peerId: String, sourceCallNumber: Int) {
                Log.d(TAG, "IAX incoming call from $peerId, call number: $sourceCallNumber")
                incomingCallNumber = sourceCallNumber
                currentPeerId = peerId
                onIncomingIaxCall?.invoke(peerId, sourceCallNumber)
            }
        })
    }

    /**
     * Start outgoing call (caller initiates)
     */
    fun startOutgoingCall(peerId: String) {
        Log.d(TAG, "Starting outgoing call to $peerId")
        currentPeerId = peerId
        
        // Send MQTT call request (optional signaling)
        mqtt.sendCallRequest(peerId)
        
        // Configure audio routing
        routeAudio()
        
        // Start IAX call immediately
        iax.startCall(peerId)
    }

    /**
     * Accept incoming IAX call (callee answers)
     */
    fun acceptIncomingCall() {
        currentPeerId?.let { peerId ->
            if (incomingCallNumber == 0) {
                Log.e(TAG, "No incoming call number to accept")
                return
            }
            
            Log.d(TAG, "Accepting incoming IAX call from $peerId, call number: $incomingCallNumber")
            
            // Send MQTT accept (optional signaling)
            mqtt.sendCallAccept(peerId)
            
            // Configure audio routing
            routeAudio()
            
            // ✅ CORRECT: Accept the existing IAX call (not start new!)
            iax.acceptCall(incomingCallNumber)
        }
    }

    /**
     * Reject incoming call
     */
    fun rejectCall() {
        currentPeerId?.let { peerId ->
            Log.d(TAG, "Rejecting call from $peerId")
            mqtt.sendCallReject(peerId)
            // IAX will auto-reject when we don't answer
        }
        resetCall()
    }

    /**
     * End current active call
     */
    fun endCall() {
        currentPeerId?.let { peerId ->
            Log.d(TAG, "Ending call with $peerId")
            mqtt.sendCallEnd(peerId)
            iax.hangup()
        }
        resetCall()
    }

    /**
     * Called when we receive CALL_ACCEPT from peer via MQTT
     * (Peer accepted our outgoing call request)
     */
    fun onCallAcceptedViaMqtt(peerId: String) {
        Log.d(TAG, "Peer $peerId accepted our call via MQTT")
        // Audio already started when we initiated the IAX call
        // Nothing to do here except maybe update UI
    }

    /**
     * Called when we receive CALL_END from peer via MQTT
     */
    fun onCallEndedViaMqtt() {
        Log.d(TAG, "Received CALL_END via MQTT, hanging up")
        endCall()
    }

    /**
     * Configure audio routing for voice call
     */
    private fun routeAudio() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
        Log.d(TAG, "Audio routed for communication mode")
    }

    /**
     * Reset call state
     */
    private fun resetCall() {
        currentPeerId = null
        incomingCallNumber = 0
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
    
    /**
     * Check if there's an incoming call ringing
     */
    fun hasIncomingCall(): Boolean {
        return iax.getCallState() == IaxManager.CallState.RINGING
    }
}