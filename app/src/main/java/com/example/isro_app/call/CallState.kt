package com.example.isro_app.call

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class CallUiState {
    IDLE,
    OUTGOING,
    INCOMING,
    CONNECTED
}

object CallState {

    private val _uiState = MutableStateFlow(CallUiState.IDLE)
    val uiState: StateFlow<CallUiState> = _uiState

    private val _activePeer = MutableStateFlow<String?>(null)
    val activePeer: StateFlow<String?> = _activePeer

    fun outgoingCall(peerId: String) {
        _activePeer.value = peerId
        _uiState.value = CallUiState.OUTGOING
    }

    fun incomingCall(peerId: String) {
        _activePeer.value = peerId
        _uiState.value = CallUiState.INCOMING
    }

    fun callConnected() {
        _uiState.value = CallUiState.CONNECTED
    }

    fun callEnded() {
        _activePeer.value = null
        _uiState.value = CallUiState.IDLE
    }
}
