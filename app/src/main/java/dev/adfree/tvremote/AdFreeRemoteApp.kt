package dev.adfree.tvremote

import android.app.Application
import dev.adfree.tvremote.service.RemoteSessionManager

class AdFreeRemoteApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize the singleton session manager
        RemoteSessionManager.getInstance(this)
    }
}

