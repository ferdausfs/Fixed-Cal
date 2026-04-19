# FTT Signal — Native Android App

WebView-based native wrapper for the FTT Signal v6.5.x HTML app.  
HTML → WebView → AndroidBridge → Native features (notifications, vibration, background scan).

---

## Project Setup

### 1. HTML file রাখো
```
app/src/main/assets/index.html
```
তোমার `v_6_5_2_otc.html` এই path-এ রেনেম করে রাখো।

### 2. GitHub repo-তে push করো
```bash
git init
git remote add origin https://github.com/YOUR_USER/FTTSignalApp.git
git add .
git commit -m "Initial commit"
git push -u origin main
```
Push হওয়ার সাথে সাথে GitHub Actions automatically debug APK build করবে।

### 3. APK ডাউনলোড
`Actions` tab → latest workflow run → `Artifacts` section → **FTTSignal-debug-N** download করো।

---

## Gradle Wrapper Setup (একবারই করতে হবে)

Local machine বা Termux-এ:
```bash
# Gradle 8.4 দিয়ে wrapper generate করো
gradle wrapper --gradle-version 8.4
# অথবা existing ./gradlew থাকলে:
./gradlew wrapper --gradle-version 8.4
```
তারপর `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` সব commit করো।

---

## AndroidBridge — HTML ↔ Native

HTML file-এ এই methods available থাকবে:

| Method | কাজ |
|--------|-----|
| `AndroidBridge.getApiBase()` | Saved API base URL return করে |
| `AndroidBridge.setApiBase(url)` | Native SharedPreferences-এ URL save করে |
| `AndroidBridge.notify(title, desc, id)` | Push notification পাঠায় |
| `AndroidBridge.notifPermStatus()` | `"granted"` বা `"denied"` return করে |
| `AndroidBridge.requestNotifPermission()` | OS permission dialog দেখায় |
| `AndroidBridge.vibrate(ms)` | Phone vibrate করায় |
| `AndroidBridge.startScan(pairsJson, intervalMin)` | Background watchlist scan শুরু করে |
| `AndroidBridge.stopScan()` | Scan বন্ধ করে |

---

## Features

- ✅ Full-screen WebView (edge-to-edge)
- ✅ localStorage support (journal, watchlist, settings সব save হয়)
- ✅ Push notifications (BUY/SELL alerts)
- ✅ Haptic vibration on signal
- ✅ Background watchlist scanner (Foreground Service)
- ✅ External links (Olymp Trade) system browser-এ খোলে
- ✅ Back button WebView history navigation

---

## Package
`com.ftt.signal` / `com.ftt.signal.debug` (debug build)
