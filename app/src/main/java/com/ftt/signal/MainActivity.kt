package com.ftt.signal

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var bridge: AndroidBridge

    companion object {
        private const val NOTIF_PERM_CODE = 101
        var instance: MainActivity? = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        // ── Edge-to-edge + dark system bars ──────────────────
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // ── WebView ───────────────────────────────────────────
        webView = WebView(this)
        setContentView(webView)

        // ── Window insets → HTML-এ inject করো ────────────────
        // Status bar height → #topbar এর padding-top
        // Navigation bar height → #navbar এর padding-bottom
        ViewCompat.setOnApplyWindowInsetsListener(webView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val statusBarPx = systemBars.top      // px
            val navBarPx    = systemBars.bottom   // px

            // px → dp conversion (WebView uses CSS px ≈ dp on most devices)
            val density = resources.displayMetrics.density
            val statusBarDp = (statusBarPx / density).toInt()
            val navBarDp    = (navBarPx    / density).toInt()

            // Store for injection after page load
            webView.tag = Pair(statusBarDp, navBarDp)

            insets
        }

        // ── WebView settings ──────────────────────────────────
        val ws: WebSettings = webView.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true
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

        // ── AndroidBridge ─────────────────────────────────────
        bridge = AndroidBridge(this)
        webView.addJavascriptInterface(bridge, "AndroidBridge")

        // ── WebViewClient ─────────────────────────────────────
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                if (!url.startsWith("file://") &&
                    !url.contains("umuhammadiswa.workers.dev")
                ) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                // ── Insets inject ─────────────────────────────
                val pair = webView.tag as? Pair<*, *>
                val statusBarDp = (pair?.first as? Int) ?: 0
                val navBarDp    = (pair?.second as? Int) ?: 0

                // #topbar কে status bar-এর নিচে নামাও
                // #navbar কে navigation bar-এর উপরে রাখো
                val js = """
                    (function() {
                        var topbar = document.getElementById('topbar');
                        var navbar = document.getElementById('navbar');
                        if (topbar) {
                            topbar.style.paddingTop = '${statusBarDp}px';
                            // topbar height adjust করো
                            var curH = parseInt(window.getComputedStyle(topbar).height) || 62;
                            topbar.style.height = (curH + ${statusBarDp}) + 'px';
                        }
                        if (navbar) {
                            navbar.style.paddingBottom =
                                'calc(' + ${navBarDp} + 'px + env(safe-area-inset-bottom, 0px))';
                        }
                        // CSS variable হিসেবেও set করো (future-proof)
                        document.documentElement.style.setProperty(
                            '--android-status-bar', '${statusBarDp}px');
                        document.documentElement.style.setProperty(
                            '--android-nav-bar', '${navBarDp}px');
                    })();
                """.trimIndent()

                view.evaluateJavascript(js, null)
            }
        }

        // ── Load ──────────────────────────────────────────────
        webView.loadUrl("file:///android_asset/index.html")

        // ── Notification permission (Android 13+) ─────────────
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
