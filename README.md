# AdFreeTVRemote 📺📱

A 100% free, open-source, private, and ad-free Android TV Remote controller application built with modern **Jetpack Compose**, **Kotlin Coroutines**, and **Material Design 3**.

---

## 🚀 Key Features

- 🚫 **100% Free & Ad-Free**: Zero trackers, zero SDK bloat, zero analytics, zero in-app purchases or paywalls.
- 🔒 **Privacy-First & Secure**: Strict TLS certificate generation, no cleartext traffic (`usesCleartextTraffic="false"`), no unnecessary permissions.
- 🎮 **Dual Remote Modes**:
  - **D-Pad Mode**: Traditional 5-way directional pad (Up, Down, Left, Right, OK) with tactile haptic feedback.
  - **Trackpad Mode**: Swipe-to-navigate gesture trackpad with tap-to-select.
- 📱 **Interactive Home Screen Widget**:
  - Full remote control widget directly on your Android home screen!
  - Control your TV without opening the app: D-Pad, OK, Back, Home, and Power.
- ⌨️ **Text & Keyboard Input**: Send text, searches, and URLs directly to TV input fields.
- 🔊 **Complete Media Controls**: Volume Up/Down, Mute, Play/Pause, Channel Up/Down, Home, Back, Menu, Power.
- 📡 **Instant TV Discovery**: Auto-detects Android TVs on your local Wi-Fi network via mDNS (`_androidtvremote2._tcp`).

---

## 📦 Download APK

Pre-built debug APKs are available at:
- `C:\Users\kakar\Downloads\AdFreeTVRemote-debug.apk`
- `c:\Users\kakar\OneDrive\Documents\GitHub\AdFreeTVRemote\AdFreeTVRemote-debug.apk`

### Installation via ADB:
```powershell
adb install -r "C:\Users\kakar\Downloads\AdFreeTVRemote-debug.apk"
```

---

## 🛠️ Security & Architecture Comparison

| Metric | Original Target APK (`base (2).apk`) | AdFreeTVRemote |
| :--- | :--- | :--- |
| **Monetization** | Google Play Billing, Paywalls, Trial locks | **100% Free, No Paywalls** |
| **Advertising** | 12+ Ad Networks (AdMob, AppLovin, IronSource, etc.) | **Zero Ads, Zero Trackers** |
| **Permissions** | 24 (Location, Phone state, Ad ID, etc.) | **6 (Only Wi-Fi, Internet & Vibrate)** |
| **Cleartext Traffic**| Allowed (`usesCleartextTraffic="true"`) | **Strictly Disabled (TLS Only)** |
| **Dynamic Code Loading**| Yes (Dex in assets/ loaded dynamically) | **None (Pure static compilation)** |
| **Home Screen Widget**| None | **Interactive D-Pad & Trackpad Widget** |
| **Target SDK** | Android 14 (34) | **Android 15 (35)** |

---

## 🧩 Home Screen Widget Setup

1. Long-press on any empty area of your Android home screen.
2. Select **Widgets**.
3. Scroll to **AdFree TV Remote**.
4. Drag and place the widget on your home screen.
5. Use the navigation buttons to control your Android TV instantly!

---

## 🔨 Building from Source

### Prerequisites:
- Android Studio Ladybug / Meerkat or Android SDK 35
- JDK 17 or JDK 22
- Gradle 8.10+ / 9.5

```powershell
.\gradlew.bat assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`
