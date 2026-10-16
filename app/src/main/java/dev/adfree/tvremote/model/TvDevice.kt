package dev.adfree.tvremote.model

data class TvDevice(
    val name: String,
    val host: String,
    val port: Int = 6466,
    val pairingPort: Int = 6467,
    val isPaired: Boolean = false,
    val isManual: Boolean = false
) {
    val id: String get() = "$host:$port"
}

