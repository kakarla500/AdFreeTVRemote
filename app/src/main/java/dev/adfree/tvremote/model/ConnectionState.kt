package dev.adfree.tvremote.model

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Searching : ConnectionState
    data class Connecting(val device: TvDevice) : ConnectionState
    data class PairingRequired(val device: TvDevice) : ConnectionState
    data class Connected(val device: TvDevice) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

