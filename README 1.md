# FTT Signal — Android App

> Professional Forex/Crypto trading signal app for Olymp Trade, wrapped as a native Android application.

---

## ✨ Features

- 📡 Live signal fetching (BUY / SELL / WAIT) across 3 timeframes
- 🕐 OTC pair support with dedicated endpoint
- 👁 Watchlist scanner with configurable interval
- 📔 Trade journal with auto P&L resolution
- 📊 Analytics dashboard (win rate, session stats, pair performance)
- 🔔 Native push notifications for new signals
- 📳 Haptic vibration on BUY/SELL
- ⚡ Foreground service keeps scanning alive in background
- 🌙 Material You dark theme (AMOLED optimised)

---

## 🚀 Getting the APK

### Option A — GitHub Actions (Recommended)

1. Push to `main` or `master` branch
2. Go to **Actions** tab → **Build Debug APK**
3. Wait ~3–4 minutes for the build
4. Download the APK from **Artifacts**

For a tagged release APK:
```bash
git tag v6.5.0
git push origin v6.5.0
```
This creates a GitHub Release with the APK attached.

### Option B — Build locally

Requirements: Android Studio Hedgehog+ or JDK 17 + Android SDK

```bash
# Clone
git clone https://github.com/YOUR_USERNAME/ftt-signal-android.git
cd ftt-signal-android

# Build debug APK
./gradlew assembleDebug

# APK output:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📁 Project Structure

```
ftt-signal-android/
├── .github/workflows/
│   └── build-apk.yml          ← GitHub Actions CI
├── app/src/main/
│   ├── assets/
│   │   └── index.html         ← Full app UI (HTML/CSS/JS)
│   ├── java/com/ftt/signal/
│   │   ├── FttApp.kt          ← Application class + notification channels
│   │   ├── MainActivity.kt    ← WebView host activity
│   │   ├── AndroidBridge.kt   ← JS ↔ Native interface
│   │   └── ScanService.kt     ← Foreground service for background scan
│   ├── res/
│   │   ├── drawable/          ← Vector icons
│   │   ├── mipmap-*/          ← Adaptive launcher icons
│   │   ├── values/            ← Strings, colors, themes
│   │   └── xml/               ← Network security, backup rules
│   └── AndroidManifest.xml
├── app/build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🔧 Configuration

### Change API URL
Inside the app → ⚙️ Settings → enter your Worker URL → Save.

The URL persists across sessions in `SharedPreferences`.

### Default API
```
https://fttotcv6.umuhammadiswa.workers.dev
```

---

## 📲 Install APK on Android

```bash
# Via ADB
adb install app-debug.apk

# Or: transfer APK to phone → open Files app → tap APK
# (Enable "Install from unknown sources" in Settings → Security)
```

---

## 🛡️ Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | Fetch signals from Worker API |
| `VIBRATE` | Haptic feedback on BUY/SELL |
| `POST_NOTIFICATIONS` | Signal alert notifications |
| `FOREGROUND_SERVICE` | Keep watchlist scanner alive |
| `WAKE_LOCK` | Prevent CPU sleep during scan |

---

## 🧩 AndroidBridge API

The HTML calls these methods on `window.AndroidBridge`:

| Method | Returns | Description |
|---|---|---|
| `getApiBase()` | `String` | Saved API base URL |
| `vibrate(ms)` | — | Native vibration |
| `notify(title, body, id)` | — | Push notification |
| `notifPermStatus()` | `"granted"\|"denied"` | Check permission |
| `requestNotifPermission()` | — | Request permission dialog |
| `startScan(pairsJson, interval)` | — | Start foreground scan service |
| `stopScan()` | — | Stop service |

---

## 📦 Tech Stack

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0+)
- **Target SDK**: 34 (Android 14)
- **WebView**: System WebView (auto-updated via Play Store)
- **Build System**: Gradle 8.6 + AGP 8.3.0

---

## 📝 License

Private — Okla's project. All rights reserved.
