package dev.adfree.tvremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.adfree.tvremote.service.RemoteSessionManager
import dev.adfree.tvremote.service.TvRemoteService
import dev.adfree.tvremote.ui.RemoteScreen
import dev.adfree.tvremote.ui.theme.AdFreeTVRemoteTheme

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: RemoteSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionManager = RemoteSessionManager.getInstance(this)
        sessionManager.startScanning()

        setContent {
            AdFreeTVRemoteTheme {
                RemoteScreen(sessionManager = sessionManager)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sessionManager.updateWidgets()
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionManager.stopScanning()
    }
}

