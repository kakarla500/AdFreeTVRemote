package dev.adfree.tvremote.protocol

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import dev.adfree.tvremote.model.TvDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress

class AndroidTvDiscovery(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredDevices = MutableStateFlow<Map<String, TvDevice>>(emptyMap())
    val discoveredDevices: StateFlow<Map<String, TvDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    companion object {
        private const val TAG = "TvDiscovery"
        private const val SERVICE_TYPE_V2 = "_androidtvremote2._tcp."
        private const val SERVICE_TYPE_V1 = "_androidtvremote._tcp."
    }

    fun startDiscovery() {
        if (_isScanning.value) return

        try {
            multicastLock = wifiManager?.createMulticastLock("AdFreeTVRemote:mDNS")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire multicast lock: ${e.message}")
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started: $regType")
                _isScanning.value = true
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName} (${service.serviceType})")
                resolveService(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${service.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
                _isScanning.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed to start: Error code $errorCode")
                _isScanning.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed to stop: Error code $errorCode")
                _isScanning.value = false
            }
        }

        discoveryListener = listener
        try {
            nsdManager?.discoverServices(SERVICE_TYPE_V2, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service discovery: ${e.message}")
            _isScanning.value = false
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        nsdManager?.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "Resolve failed: Error code $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.host?.hostAddress ?: return
                val port = serviceInfo.port
                val name = cleanServiceName(serviceInfo.serviceName)

                val device = TvDevice(
                    name = name,
                    host = host,
                    port = if (port > 0) port else 6466,
                    pairingPort = 6467
                )

                _discoveredDevices.value = _discoveredDevices.value + (device.id to device)
                Log.i(TAG, "TV Discovered: $name at $host:$port")
            }
        })
    }

    fun addManualDevice(ipAddress: String, customName: String = "Android TV") {
        val trimmed = ipAddress.trim()
        if (trimmed.isEmpty()) return
        val device = TvDevice(
            name = customName,
            host = trimmed,
            port = 6466,
            pairingPort = 6467,
            isManual = true
        )
        _discoveredDevices.value = _discoveredDevices.value + (device.id to device)
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager?.stopServiceDiscovery(it)
            } catch (_: Exception) {}
        }
        discoveryListener = null
        _isScanning.value = false

        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {}
        multicastLock = null
    }

    private fun cleanServiceName(raw: String): String {
        return raw.replace("\\032", " ").replace("\\", "").trim()
    }
}

