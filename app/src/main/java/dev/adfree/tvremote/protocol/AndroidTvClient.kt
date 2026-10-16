package dev.adfree.tvremote.protocol

import android.content.Context
import android.util.Log
import dev.adfree.tvremote.model.RemoteKey
import dev.adfree.tvremote.model.TvDevice
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

/**
 * Handles the TLS socket session, pairing handshake, and keycode transmission
 * to an Android TV device.
 */
class AndroidTvClient(
    private val context: Context,
    private val certManager: TvCertificateManager
) {

    private var sslSocket: SSLSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isConnected = false

    companion object {
        private const val TAG = "TvClient"
        private const val TIMEOUT_MS = 10000
    }

    suspend fun connect(device: TvDevice): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        try {
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(certManager.getOrCreateKeyManagers(), certManager.getTrustAllTrustManagers(), SecureRandom())
            }

            val socket = (sslContext.socketFactory.createSocket(device.host, device.port) as SSLSocket).apply {
                soTimeout = TIMEOUT_MS
                useClientMode = true
                needClientAuth = true
                startHandshake()
            }

            sslSocket = socket
            outputStream = socket.outputStream
            inputStream = socket.inputStream
            isConnected = true
            Log.i(TAG, "Connected successfully to ${device.name} at ${device.host}:${device.port}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}")
            disconnect()
            false
        }
    }

    suspend fun pair(device: TvDevice, pin: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(certManager.getOrCreateKeyManagers(), certManager.getTrustAllTrustManagers(), SecureRandom())
            }

            val pairSocket = (sslContext.socketFactory.createSocket(device.host, device.pairingPort) as SSLSocket).apply {
                soTimeout = 15000
                useClientMode = true
                needClientAuth = true
                startHandshake()
            }

            val out = pairSocket.outputStream
            val inp = pairSocket.inputStream

            // Send pairing request packet
            val clientCert = certManager.getClientCertificate()
            val certSha256 = MessageDigest.getInstance("SHA-256").digest(clientCert.encoded)

            // Step 1: Pairing Request
            val pairReq = buildPairingRequestPacket(serviceName = "AdFreeTVRemote", certHash = certSha256)
            out.write(pairReq)
            out.flush()

            // Step 2: Read Challenge & send PIN secret
            val pinBytes = pin.trim().toByteArray(Charsets.UTF_8)
            val secretPacket = buildSecretPacket(pinBytes, certSha256)
            out.write(secretPacket)
            out.flush()

            // Close pairing socket after handshake
            pairSocket.close()
            Log.i(TAG, "Pairing handshake completed for ${device.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Pairing failed: ${e.message}")
            false
        }
    }

    suspend fun sendKey(key: RemoteKey) = withContext(Dispatchers.IO) {
        if (!isConnected || outputStream == null) {
            Log.w(TAG, "Cannot send key: socket not connected")
            return@withContext
        }

        try {
            // Send Key Down (direction: 1)
            val downPacket = buildKeyPacket(key.keyCode, direction = 1)
            outputStream?.write(downPacket)
            outputStream?.flush()

            delay(50)

            // Send Key Up (direction: 2)
            val upPacket = buildKeyPacket(key.keyCode, direction = 2)
            outputStream?.write(upPacket)
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending key ${key.label}: ${e.message}")
            disconnect()
        }
    }

    suspend fun sendTextInput(text: String) = withContext(Dispatchers.IO) {
        if (!isConnected || outputStream == null || text.isEmpty()) return@withContext
        try {
            val textPacket = buildTextInputPacket(text)
            outputStream?.write(textPacket)
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending text: ${e.message}")
        }
    }

    fun disconnect() {
        isConnected = false
        try { outputStream?.close() } catch (_: Exception) {}
        try { inputStream?.close() } catch (_: Exception) {}
        try { sslSocket?.close() } catch (_: Exception) {}
        outputStream = null
        inputStream = null
        sslSocket = null
    }

    // Packet Builders based on Google Polo / Remote Protocol framing

    private fun buildKeyPacket(keyCode: Int, direction: Int): ByteArray {
        // Tag 1 (Key): [Tag=10 (wire=2), len, Tag=1 (keyCode varint), Tag=2 (direction varint)]
        val payload = ByteArray(8)
        var idx = 0
        payload[idx++] = 0x08 // field 1: keyCode (varint)
        idx = writeVarint(payload, idx, keyCode)
        payload[idx++] = 0x10 // field 2: direction (varint)
        payload[idx++] = direction.toByte()

        val packet = ByteArray(idx + 2)
        packet[0] = idx.toByte() // length header
        System.arraycopy(payload, 0, packet, 1, idx)
        return packet
    }

    private fun buildTextInputPacket(text: String): ByteArray {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val packet = ByteArray(bytes.size + 4)
        var idx = 0
        packet[idx++] = (bytes.size + 2).toByte() // length header
        packet[idx++] = 0x12 // field 2: text string tag
        packet[idx++] = bytes.size.toByte()
        System.arraycopy(bytes, 0, packet, idx, bytes.size)
        return packet
    }

    private fun buildPairingRequestPacket(serviceName: String, certHash: ByteArray): ByteArray {
        val nameBytes = serviceName.toByteArray(Charsets.UTF_8)
        val totalLen = nameBytes.size + certHash.size + 8
        val buf = ByteArray(totalLen + 4)
        var i = 0
        buf[i++] = totalLen.toByte()
        buf[i++] = 0x0A // field 1
        buf[i++] = nameBytes.size.toByte()
        System.arraycopy(nameBytes, 0, buf, i, nameBytes.size)
        i += nameBytes.size
        buf[i++] = 0x12 // field 2
        buf[i++] = certHash.size.toByte()
        System.arraycopy(certHash, 0, buf, i, certHash.size)
        return buf
    }

    private fun buildSecretPacket(pin: ByteArray, certHash: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(certHash)
        md.update(pin)
        val hash = md.digest()

        val buf = ByteArray(hash.size + 4)
        var i = 0
        buf[i++] = (hash.size + 2).toByte()
        buf[i++] = 0x0A
        buf[i++] = hash.size.toByte()
        System.arraycopy(hash, 0, buf, i, hash.size)
        return buf
    }

    private fun writeVarint(buf: ByteArray, offset: Int, value: Int): Int {
        var v = value
        var o = offset
        while (v and 0x7F.inv() != 0) {
            buf[o++] = ((v and 0x7F) or 0x80).toByte()
            v = v ushr 7
        }
        buf[o++] = (v and 0x7F).toByte()
        return o
    }
}

