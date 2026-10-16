package dev.adfree.tvremote.model

/**
 * Standard Android TV Remote Keycodes matching the Android TV Remote Protocol.
 */
enum class RemoteKey(val keyCode: Int, val label: String) {
    // Navigation
    DPAD_UP(19, "Up"),
    DPAD_DOWN(20, "Down"),
    DPAD_LEFT(21, "Left"),
    DPAD_RIGHT(22, "Right"),
    DPAD_CENTER(23, "Select"),

    // System
    BACK(4, "Back"),
    HOME(3, "Home"),
    POWER(26, "Power"),
    SETTINGS(176, "Settings"),
    APP_SWITCH(187, "Apps"),

    // Volume & Media
    VOLUME_UP(24, "Volume +"),
    VOLUME_DOWN(25, "Volume -"),
    VOLUME_MUTE(164, "Mute"),
    PLAY_PAUSE(85, "Play/Pause"),
    FAST_FORWARD(90, "Forward"),
    REWIND(89, "Rewind"),

    // Channel
    CHANNEL_UP(166, "Ch +"),
    CHANNEL_DOWN(167, "Ch -")
}

