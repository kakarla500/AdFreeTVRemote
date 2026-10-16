package dev.adfree.tvremote.protocol

import android.content.Context
import android.util.Base64
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.net.ssl.*

/**
 * Manages local self-signed TLS certificates for secure communication
 * with Android TV devices.
 */
class TvCertificateManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("adfree_tv_cert_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PRIVATE = "client_private_key"
        private const val KEY_CERTIFICATE = "client_certificate"
    }

    fun getOrCreateKeyManagers(): Array<KeyManager> {
        val (keyPair, cert) = getOrCreateCertificate()
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setKeyEntry("client", keyPair.private, "".toCharArray(), arrayOf(cert))
        }

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, "".toCharArray())
        return kmf.keyManagers
    }

    fun getTrustAllTrustManagers(): Array<TrustManager> {
        return arrayOf(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
    }

    fun getClientCertificate(): X509Certificate {
        return getOrCreateCertificate().second
    }

    private fun getOrCreateCertificate(): Pair<KeyPair, X509Certificate> {
        val privBase64 = prefs.getString(KEY_PRIVATE, null)
        val certBase64 = prefs.getString(KEY_CERTIFICATE, null)

        if (privBase64 != null && certBase64 != null) {
            try {
                val keyBytes = Base64.decode(privBase64, Base64.DEFAULT)
                val certBytes = Base64.decode(certBase64, Base64.DEFAULT)

                val kf = KeyFactory.getInstance("RSA")
                val privKey = kf.generatePrivate(PKCS8EncodedKeySpec(keyBytes))

                val certFactory = CertificateFactory.getInstance("X.509")
                val cert = certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate

                val keyPair = KeyPair(cert.publicKey, privKey)
                return Pair(keyPair, cert)
            } catch (_: Exception) {
                // If corrupted, regenerate
            }
        }

        return generateAndSaveNewCertificate()
    }

    private fun generateAndSaveNewCertificate(): Pair<KeyPair, X509Certificate> {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val keyPair = keyGen.generateKeyPair()

        val cert = generateSelfSignedCert(keyPair)

        prefs.edit()
            .putString(KEY_PRIVATE, Base64.encodeToString(keyPair.private.encoded, Base64.DEFAULT))
            .putString(KEY_CERTIFICATE, Base64.encodeToString(cert.encoded, Base64.DEFAULT))
            .apply()

        return Pair(keyPair, cert)
    }

    /**
     * Generates a minimal self-signed X.509 certificate for mutual TLS handshake with Android TV.
     */
    private fun generateSelfSignedCert(keyPair: KeyPair): X509Certificate {
        // Build self-signed certificate using standard Java ASN.1 der encoding
        val startDate = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        val expiryDate = Date(System.currentTimeMillis() + 10 * 365 * 24 * 60 * 60 * 1000L) // 10 years

        val certBytes = createSelfSignedCertDer(keyPair, startDate, expiryDate).toByteArray()
        val certFactory = CertificateFactory.getInstance("X.509")
        return certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
    }

    private fun createSelfSignedCertDer(keyPair: KeyPair, notBefore: Date, notAfter: Date): ByteArrayList {
        // Minimal valid self-signed DER X.509 v3
        val der = ByteArrayList()

        // TBSCertificate
        val tbs = ByteArrayList()
        // Version v3 (0x02)
        tbs.append(0xA0, 0x03, 0x02, 0x01, 0x02)
        // Serial number
        tbs.append(0x02, 0x08, 0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF)
        // Signature Algorithm SHA256withRSA: 1.2.840.113549.1.1.11
        val sigAlg = byteArrayOf(
            0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86.toByte(), 0x48, 0x86.toByte(),
            0xF7.toByte(), 0x0D, 0x01, 0x01, 0x0B, 0x05, 0x00
        )
        tbs.append(sigAlg)

        // Issuer & Subject: CN=AdFreeTVRemote
        val nameDer = byteArrayOf(
            0x30, 0x1A, 0x31, 0x18, 0x30, 0x16, 0x06, 0x03, 0x55, 0x04, 0x03,
            0x0C, 0x0F, 'A'.code.toByte(), 'd'.code.toByte(), 'F'.code.toByte(),
            'r'.code.toByte(), 'e'.code.toByte(), 'e'.code.toByte(), 'T'.code.toByte(),
            'V'.code.toByte(), 'R'.code.toByte(), 'e'.code.toByte(), 'm'.code.toByte(),
            'o'.code.toByte(), 't'.code.toByte(), 'e'.code.toByte(), 0x31
        )
        tbs.append(nameDer) // Issuer

        // Validity
        val timeFormat = java.text.SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val notBeforeBytes = timeFormat.format(notBefore).toByteArray()
        val notAfterBytes = timeFormat.format(notAfter).toByteArray()
        val validity = ByteArrayList()
        validity.append(0x17, notBeforeBytes.size)
        validity.append(notBeforeBytes)
        validity.append(0x17, notAfterBytes.size)
        validity.append(notAfterBytes)
        tbs.append(0x30, validity.size)
        tbs.append(validity.toByteArray())

        tbs.append(nameDer) // Subject

        // SubjectPublicKeyInfo
        val pubKeyBytes = keyPair.public.encoded
        tbs.append(pubKeyBytes)

        val tbsBytes = ByteArrayList()
        tbsBytes.append(0x30)
        tbsBytes.appendLength(tbs.size)
        tbsBytes.append(tbs.toByteArray())

        // Sign TBSCertificate
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(keyPair.private)
        sig.update(tbsBytes.toByteArray())
        val signatureBytes = sig.sign()

        // Assembly: Sequence { tbs, sigAlg, BitString(signature) }
        val certSeq = ByteArrayList()
        certSeq.append(tbsBytes.toByteArray())
        certSeq.append(sigAlg)

        certSeq.append(0x03) // BIT STRING
        certSeq.appendLength(signatureBytes.size + 1)
        certSeq.append(0x00) // unused bits
        certSeq.append(signatureBytes)

        val fullDer = ByteArrayList()
        fullDer.append(0x30)
        fullDer.appendLength(certSeq.size)
        fullDer.append(certSeq.toByteArray())

        return fullDer
    }

    private class ByteArrayList {
        private var data = ByteArray(64)
        var size = 0
            private set

        fun append(vararg bytes: Int) {
            for (b in bytes) append(b.toByte())
        }

        fun append(byte: Byte) {
            ensureCapacity(size + 1)
            data[size++] = byte
        }

        fun append(bytes: ByteArray) {
            ensureCapacity(size + bytes.size)
            System.arraycopy(bytes, 0, data, size, bytes.size)
            size += bytes.size
        }

        fun appendLength(len: Int) {
            if (len < 128) {
                append(len)
            } else if (len <= 0xFF) {
                append(0x81, len)
            } else {
                append(0x82, (len shr 8) and 0xFF, len and 0xFF)
            }
        }

        private fun ensureCapacity(cap: Int) {
            if (cap > data.size) {
                data = data.copyOf(maxOf(data.size * 2, cap))
            }
        }

        fun toByteArray(): ByteArray = data.copyOf(size)
    }
}

