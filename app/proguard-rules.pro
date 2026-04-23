# FTT Signal ProGuard rules

# Keep WebView JS interface methods
-keepclassmembers class com.ftt.signal.AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep all public members in our package
-keep public class com.ftt.signal.** { *; }

# AndroidX
-keep class androidx.** { *; }

# Suppress warnings
-dontwarn java.lang.invoke.*
