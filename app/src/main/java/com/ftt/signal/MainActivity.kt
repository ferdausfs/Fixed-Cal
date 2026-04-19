package com.ftt.signal

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var bridge: AndroidBridge

    companion object {
        private const val NOTIF_PERM_CODE = 101
        // Singleton so ScanService can call back
        var instance: MainActivity? = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        // Edge-to-edge: full dark immersive
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.parseColor("#07080e")
        window.navigationBarColor = Color.parseColor("#07080e")
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        // Keep screen on while app is foreground
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Create WebView programmatically (no XML layout needed)
        webView = WebView(this)
        webView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
        setContentView(webView)

        // ── WebView settings ──────────────────────────────
        val ws: WebSettings = webView.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true          // localStorage
        ws.databaseEnabled = true
        ws.allowFileAccess = true
        ws.allowContentAccess = true
        ws.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        ws.cacheMode = WebSettings.LOAD_DEFAULT
        ws.loadsImagesAutomatically = true
        ws.useWideViewPort = true
        ws.loadWithOverviewMode = true
        ws.setSupportZoom(false)
        ws.builtInZoomControls = false
        ws.displayZoomControls = false
        // Enable hardware acceleration in WebView
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // ── AndroidBridge ─────────────────────────────────
        bridge = AndroidBridge(this)
        webView.addJavascriptInterface(bridge, "AndroidBridge")

        // ── WebViewClient: সব link এই same WebView-এ খুলবে ─
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                // External broker links (Olymp Trade, Quotex) → system browser
                if (!url.startsWith("file://") &&
                    !url.contains("umuhammadiswa.workers.dev") &&
                    !url.contains("cloudflare")) {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(url)
                    )
                    startActivity(intent)
                    return true
                }
                return false
            }
        }

        // ── Load the app ──────────────────────────────────
        webView.loadUrl("file:///android_asset/index.html")

        // ── Notification permission (Android 13+) ─────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIF_PERM_CODE
                )
            }
        }
    }

    /** Called from AndroidBridge.requestNotifPermission() */
    fun requestNotifPermFromBridge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIF_PERM_CODE
            )
        }
    }

    override fun onBackPressed() {
        // WebView back navigation
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.pauseTimers()
    }

    override fun onDestroy() {
        instance = null
        webView.destroy()
        super.onDestroy()
    }
}
