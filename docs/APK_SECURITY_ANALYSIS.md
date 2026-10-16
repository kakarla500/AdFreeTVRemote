# Static APK Analysis & Security Assessment Report

**Target:** `base (2).apk`  
**Package:** `dev.niamor.androidtvremote`  
**Application Label:** Android TV Remote  
**Analysis Date:** 2026-10-16  
**Analyzer:** Antigravity REA Engine & Android AAPT Security Suite  

---

## 1. Executive Summary

A comprehensive static analysis and security audit was performed on the Android application package (`base (2).apk`). The target is an Android TV Remote controller application built using Jetpack Compose, Jetpack Glance for home screen widgets, and the Google Polo pairing and control protocol.

While the core remote functionality is relatively lightweight, over **85% of the application footprint** consists of 12+ third-party advertising SDKs, mediation adapters, analytics trackers, and Google Play In-App Billing libraries. Several security vulnerabilities and insecure configurations were identified, including permissive cleartext HTTP traffic, exposed third-party broadcast receivers, and hardcoded API credentials.

---

## 2. Package & Build Artifact Overview

| Property | Value |
| :--- | :--- |
| **Package Name** | `dev.niamor.androidtvremote` |
| **Version Name** | `2.1.4` |
| **Version Code** | `211` |
| **File Size** | `57,763,023 bytes` (~55.1 MB) |
| **SHA-256** | `0720b8914529b0cfa4b7f14980f5c404138fc89ac41aa944a17e8d5c609c2d26` |
| **Minimum SDK** | `32` (Android 12L / Android 13) |
| **Target SDK** | `36` (Android 16 Preview / Android 15+) |
| **Compile SDK** | `36` |
| **Native Binaries (`lib/`)** | None (100% Pure DEX / Dalvik bytecode) |
| **DEX Count** | 6 root DEX files (`classes.dex` to `classes6.dex`) + 2 asset DEX files |

---

## 3. Application Architecture & Project Structure

The decompiled DEX structure reveals a modularized modern Android stack:

```
dev.niamor
├── androidtv
│   └── (Resources and TV protocol bindings)
├── androidtvremote
│   ├── RemoteApplication (Application class)
│   ├── ui
│   │   ├── MainActivity (Main launchable activity)
│   │   └── Hilt_MainActivity (Dagger Hilt injection wrapper)
│   └── widget.glance
│       ├── SetRemoteModeAction
│       └── SetStackedModeAction
├── boxremote
│   ├── database
│   │   ├── apps.local.AppsDatabase (Room database for installed TV apps)
│   │   └── epg.local.EpgDatabase (Room database for TV guide / EPG)
│   ├── model (ThemeMode, RemoteMode, AccentId, RemoteLayout)
│   ├── notifications.MyFirebaseMessagingService (Push notification receiver)
│   └── widget.glance
│       ├── AndroidTvRemoteService (Foreground service for active TV connection)
│       ├── SendCommandAction (Sends D-pad/key events to TV)
│       ├── ZapChannelAction (Quick channel switcher)
│       └── LaunchDrivenTvAppAction (Launch streaming apps on TV)
└── core
    ├── advertising (Ad mediation wrappers)
    ├── billing (Google Play Billing client integration)
    └── remoteconfig (Firebase Remote Config for dynamic feature flags)
```

### Core Communication Mechanism
- **TV Pairing & Control**: Implements Google's **Polo Protocol** (`com.google.polo`) combined with **Protocol Buffers** (`com.google.protobuf`).
- **Discovery**: Wi-Fi multicast (`CHANGE_WIFI_MULTICAST_STATE`) and local network socket communication to discover Android TV devices via mDNS / SSDP.
- **Widgets**: Uses **Android Jetpack Glance** with Compose-driven GlanceReceivers (`StackedGlanceReceiver`, `ArrowsGlanceReceiver`, `FunctionsGlanceReceiver`, `AppsListGlanceReceiver`).

---

## 4. Permissions Review

### High-Privilege & Sensitive Permissions
| Permission | Risk Level | Context & Justification |
| :--- | :--- | :--- |
| `android.permission.RECORD_AUDIO` | **HIGH** | Required for Voice Remote input and Google Assistant commands. |
| `android.permission.FOREGROUND_SERVICE` | **MEDIUM** | Keeps the remote connection alive in the background. |
| `android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE` | **MEDIUM** | Required on Android 14+ for foreground services managing connected external devices. |
| `android.permission.BLUETOOTH_CONNECT` | **MEDIUM** | Direct Bluetooth connection to Android TV devices. |
| `android.permission.CHANGE_WIFI_MULTICAST_STATE` | **LOW-MEDIUM** | Required for mDNS service discovery of TVs on local Wi-Fi. |

### Ad & Tracking Permissions
| Permission | Purpose |
| :--- | :--- |
| `com.google.android.gms.permission.AD_ID` | Google Play Advertising ID access. |
| `android.permission.ACCESS_ADSERVICES_AD_ID` | Android Privacy Sandbox Ad ID. |
| `android.permission.ACCESS_ADSERVICES_ATTRIBUTION` | Attribution reporting for ad conversions. |
| `android.permission.ACCESS_ADSERVICES_TOPICS` | Topics API for interest-based advertising. |
| `com.android.vending.BILLING` | Google Play In-App Billing for premium purchases. |

---

## 5. Components & Exported Surface Analysis

### Exported Components
| Component Type | Component Name | Protection | Risk / Notes |
| :--- | :--- | :--- | :--- |
| **Activity** | `dev.niamor.androidtvremote.ui.MainActivity` | None (Public Launcher) | Legitimate app launcher activity. |
| **Activity** | `org.prebid.mobile.rendering.views.browser.AdBrowserActivity` | **None** | **Exposed**: Third-party ad browser activity can be directly launched by external apps. |
| **Receiver** | `com.ogury.core.internal.LogEnablerReceiver` | **None** | **Exposed**: Third-party ad receiver allows external apps to trigger logging. |
| **Receiver** | `com.mbridge.msdk.foundation.same.broadcast.NetWorkChangeReceiver` | **None** | **Exposed**: Mintegral ad receiver listens to network broadcast intents. |
| **Receiver** | `androidx.profileinstaller.ProfileInstallReceiver` | `android.permission.DUMP` | Protected system receiver. |
| **Service** | `androidx.glance.appwidget.GlanceRemoteViewsService` | `BIND_REMOTEVIEWS` | Protected Android widget service. |
| **Service** | `androidx.work.impl.background.systemjob.SystemJobService` | `BIND_JOB_SERVICE` | Protected JobScheduler service. |

---

## 6. Embedded Libraries & Ad SDK Census

Static scanning identified heavy ad mediation bundling over 12 ad networks:

1. **Ad Mediation**: AppLovin MAX SDK (`com.applovin.*`)
2. **Google AdMob & Firebase**: (`com.google.android.gms.ads`, `com.google.firebase.*`)
3. **Meta Audience Network**: (`com.facebook.ads`, plus dynamic dex payloads in assets)
4. **InMobi Ads**: (`com.inmobi.ads`, `com.inmobi.rendering`)
5. **BidMachine**: (`io.bidmachine.*`)
6. **Liftoff / Vungle**: (`com.vungle.ads.*`)
7. **Unity / IronSource LevelPlay**: (`com.ironsource.*`, `com.unity3d.*`)
8. **Digital Turbine / Fyber**: (`com.fyber.inneractive.*`, `com.digitalturbine.ignite`)
9. **Mintegral / MBridge**: (`com.mbridge.msdk.*`)
10. **Ogury**: (`com.ogury.ad.*`)
11. **Moloco Ads**: (`com.moloco.sdk.*`)
12. **Prebid Mobile**: (`org.prebid.mobile.*`)
13. **ByteDance / Pangle**: (`com.bytedance.pangle.*`)
14. **Amazon Publisher Services**: (`com.amazon.aps`, `com.amazon.privacypass`)

---

## 7. Network Endpoints & External APIs

### Application Endpoints
- `https://niamor.dev` — Main developer backend
- `https://niamor.dev/channels` — Remote TV guide / EPG channel metadata feed

### Insecure / Cleartext Endpoints (Found in DEX)
- `http://proxy.int.stg.x3mads.com:4000/` — Cleartext proxy endpoint
- `http://ads-test.st.ogury.com/` — Cleartext ad test endpoint
- `http://d.applovin.com/` — Cleartext ad asset delivery
- `http://res.applovin.com/` — Cleartext ad resource host

---

## 8. Security Findings & Vulnerability Assessment

### Finding 1: Permissive Cleartext HTTP Traffic
- **Severity**: **Medium** (CWE-319: Cleartext Transmission of Sensitive Information)
- **Manifest Evidence**: `android:usesCleartextTraffic="true"`
- **Impact**: Enables unencrypted HTTP connections. On modern Android versions (Android 9+), cleartext traffic is disabled by default. Enabling it exposes network requests to Man-in-the-Middle (MitM) inspection or injection on untrusted local Wi-Fi networks.
- **Remediation**: Set `android:usesCleartextTraffic="false"` and enforce strict HTTPS via a Network Security Configuration (`res/xml/network_security_config.xml`).

### Finding 2: Hardcoded API Credentials in Binary Resources
- **Severity**: **Medium-Low** (CWE-798: Use of Hard-coded Credentials)
- **Values Extracted**:
  - `google_api_key`: `[REDACTED_THIRD_PARTY_ORIGINAL_APK_KEY]`
  - `google_app_id`: `1:13936813172:android:f96fda8c0fb7a15bb91d21`
  - `gcm_defaultSenderId`: `13936813172`
  - `admob_publisher_id`: `pub-9702226135159369`
  - `admob_app_ad_id`: `ca-app-pub-9702226135159369~3280743260`
- **Impact**: While Firebase API keys are designed to identify projects, unconstrained keys can be abused to consume quotas or access backend resources if Cloud IAM restrictions are not strictly applied in the Google Cloud Console.
- **Remediation**: Restrict the API key in Google Cloud Console with package name (`dev.niamor.androidtvremote`) and SHA-1 certificate fingerprint restrictions.

### Finding 3: Exported Ad Network Receivers & Activities
- **Severity**: **Low-Medium** (CWE-926: Improper Export of Android Application Components)
- **Components**:
  - `org.prebid.mobile.rendering.views.browser.AdBrowserActivity` (`exported="true"`)
  - `com.ogury.core.internal.LogEnablerReceiver` (`exported="true"`)
  - `com.mbridge.msdk.foundation.same.broadcast.NetWorkChangeReceiver` (`exported="true"`)
- **Impact**: Other apps on the same device can send intents directly to these components without permission checks.
- **Remediation**: Ensure unneeded third-party SDK activities and receivers set `android:exported="false"` or require custom signature-level permissions.

### Finding 4: Dynamic Code Loading from Assets
- **Severity**: **Medium** (CWE-912: Hidden Functionality / Dynamic Code Loading)
- **Payloads**:
  - `assets/audience_network/classes.dex` (57 KB)
  - `assets/audience_network/classes2.dex` (5.2 MB)
- **Impact**: Meta Audience Network bundles secondary DEX files inside `assets/` and loads them at runtime using custom class loaders. If runtime integrity verification is inadequate, malicious or tampered code could theoretically be executed.
- **Remediation**: Avoid dynamic DEX loading; compile all application code into primary/secondary application DEX files directly verified during APK installation.
