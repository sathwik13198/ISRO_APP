package com.example.isro_app.call

import android.content.Context
import android.media.AudioManager
import com.example.iax.IaxManager
import com.example.isro_app.mqtt.MqttManager

class CallController(
    private val context: Context,
    private val mqtt: MqttManager,
    private val iax: IaxManager
) {

    fun outgoingCall(peerId: String) {
        mqtt.sendCallRequest(peerId)
    }

    fun acceptCall(peerId: String) {
        mqtt.sendCallAccept(peerId)
        // Start audio immediately when receiver accepts
        // Caller's audio will start when they receive CALL_ACCEPT (in LaunchedEffect)
        startAudio(peerId)
    }

    fun rejectCall(peerId: String) {
        mqtt.sendCallReject(peerId)
    }

    fun endCall(peerId: String) {
        mqtt.sendCallEnd(peerId)
        stopAudio()
    }

    // Called when we receive CALL_ACCEPT from peer - THIS is where audio starts
    fun onCallAccepted(peerId: String) {
        startAudio(peerId)
    }

    // Called when we receive CALL_END from peer
    fun onCallEnded() {
        stopAudio()
    }

    private fun startAudio(peerId: String) {
        routeAudio()
        iax.startCall(peerId)
    }

    private fun stopAudio() {
        iax.hangup()
    }

    private fun routeAudio() {
        val audioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
    }
}

