# Trading Signal App

Native Android app for the trading signal backend at `https://fttotcv6.umuhammadiswa.workers.dev`

## Screens
- **Status** — server health, current session (London/NY overlap), market status, signal filters
- **Signal** — live BUY/SELL signals for 18 Crypto & Forex pairs with Entry, TP, SL
- **History** — signal history with WIN/LOSS results and win rate stats

## Setup (one time after clone)

### 1. Add gradle-wrapper.jar
```bash
curl -L "https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar" \
     -o gradle/wrapper/gradle-wrapper.jar
```
Or if Gradle is installed locally:
```bash
gradle wrapper --gradle-version 8.4
```

### 2. Push to GitHub
```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git push -u origin main
```

### 3. Download APK
GitHub → **Actions** tab → `Build Debug APK` → after build completes → **Artifacts** → `debug-apk`

## Local Build
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```
