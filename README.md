# FTT Signal Native Android

This repo is a **fully native Android rewrite** of the previous WebView-based FTT Signal app.

## What changed

- Replaced the HTML/WebView shell with **Jetpack Compose** screens
- Replaced JS bridge logic with **native Kotlin** networking and background scanning
- Added a **native foreground service** for watchlist scanning
- Kept the same Worker/API flow:
  - `GET /health`
  - `GET /api/signal?pair=...`
  - `GET /api/history?pair=...&limit=...`
  - `GET /api/stats?pair=...`

## Main features

- Live signal screen for forex, crypto, and OTC pairs
- Native watchlist with background scanning service
- Local journal storage with manual WIN / LOSS tagging
- Local analytics dashboard + remote recent history preview
- Native settings screen for API base URL, notifications, vibration, scan interval

## Build

Open in Android Studio Hedgehog+ and run:

```bash
./gradlew assembleDebug
```

## Package

- Namespace: `com.ftt.signal`
- Min SDK: 26
- Target SDK: 34

## Notes

This rewrite intentionally keeps the original business API but moves UI, persistence, refresh, notifications, and watchlist scanning into native Kotlin code.
