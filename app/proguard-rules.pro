# FTT Signal — ProGuard rules
# (debug build-এ minify disabled, শুধু release-এর জন্য)

# WebView JavaScript interface — এটা remove করা যাবে না
-keepclassmembers class com.ftt.signal.AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }
