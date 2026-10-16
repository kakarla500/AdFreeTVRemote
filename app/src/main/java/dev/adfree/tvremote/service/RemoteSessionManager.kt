package dev.adfree.tvremote.service

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import dev.adfree.tvremote.model.ConnectionState
import dev.adfree.tvremote.model.RemoteKey
import dev.adfree.tvremote.model.TvDevice
import dev.adfree.tvremote.protocol.AndroidTvClient
import dev.adfree.tvremote.protocol.AndroidTvDiscovery
import dev.adfree.tvremote.protocol.TvCertificateManager
import dev.adfree.tvremote.widget.RemoteWidgetProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RemoteSessionManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val certManager = TvCertificateManager(context)
    val discovery = AndroidTvDiscovery(context)
    val client = AndroidTvClient(context, certManager)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _activeDevice = MutableStateFlow<TvDevice?>(null)
    val activeDevice: StateFlow<TvDevice?> = _activeDevice.asStateFlow()

    companion object {
        @Volatile
        private var instance: RemoteSessionManager? = null

        fun getInstance(context: Context): RemoteSessionManager {
            return instance ?: synchronized(this) {
                instance ?: RemoteSessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        // Load last connected device from shared preferences
        val prefs = context.getSharedPreferences("adfree_remote_state", Context.MODE_PRIVATE)
        val lastHost = prefs.getString("last_tv_host", null)
        val lastName = prefs.getString("last_tv_name", null)
        if (lastHost != null && lastName != null) {
            _activeDevice.value = TvDevice(name = lastName, host = lastHost)
        }
    }

    fun startScanning() {
        discovery.startDiscovery()
    }

    fun stopScanning() {
        discovery.stopDiscovery()
    }

    fun connectToDevice(device: TvDevice) {
        scope.launch {
            _activeDevice.value = device
            _connectionState.value = ConnectionState.Connecting(device)

            val success = client.connect(device)
            if (success) {
                _connectionState.value = ConnectionState.Connected(device)
                saveLastDevice(device)
                updateWidgets()
            } else {
                _connectionState.value = ConnectionState.PairingRequired(device)
            }
        }
    }

    fun pairWithPin(pin: String) {
        val device = _activeDevice.value ?: return
        scope.launch {
            _connectionState.value = ConnectionState.Connecting(device)
            val success = client.pair(device, pin)
            if (success) {
                // Now connect normal remote session
                val connected = client.connect(device)
                if (connected) {
                    _connectionState.value = ConnectionState.Connected(device)
                    saveLastDevice(device)
                    updateWidgets()
                } else {
                    _connectionState.value = ConnectionState.Error("Pairing succeeded, but session connection failed.")
                }
            } else {
                _connectionState.value = ConnectionState.Error("Incorrect PIN or pairing timed out.")
            }
        }
    }

    fun sendKey(key: RemoteKey) {
        performHaptic()
        scope.launch {
            if (_connectionState.value !is ConnectionState.Connected) {
                val last = _activeDevice.value
                if (last != null) {
                    val ok = client.connect(last)
                    if (ok) {
                        _connectionState.value = ConnectionState.Connected(last)
                        updateWidgets()
                    }
                }
            }
            client.sendKey(key)
        }
    }

    fun sendTextInput(text: String) {
        scope.launch {
            client.sendTextInput(text)
        }
    }

    fun disconnect() {
        client.disconnect()
        _connectionState.value = ConnectionState.Disconnected
        updateWidgets()
    }

    private fun performHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    private fun saveLastDevice(device: TvDevice) {
        context.getSharedPreferences("adfree_remote_state", Context.MODE_PRIVATE)
            .edit()
            .putString("last_tv_host", device.host)
            .putString("last_tv_name", device.name)
            .apply()
    }

    fun updateWidgets() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, RemoteWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        val intent = Intent(context, RemoteWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        context.sendBroadcast(intent)
    }
}

