package com.yjc.fangkuaituji

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.getcapacitor.BridgeActivity

class GameActivity : BridgeActivity() {

    private var mode = 1
    private var showSettings = false
    private var injected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = intent.getIntExtra("mode", 1)
        showSettings = intent.getBooleanExtra("showSettings", false)

        val wv = bridge?.webView
        if (wv != null) {
            val s = wv.settings
            s.javaScriptEnabled = true
            s.domStorageEnabled = true
            s.databaseEnabled = true
            s.setSupportZoom(false)
            s.builtInZoomControls = false
            s.displayZoomControls = false
            s.loadWithOverviewMode = true
            s.useWideViewPort = true
            s.mediaPlaybackRequiresUserGesture = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            wv.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (!injected) {
                        Handler(Looper.getMainLooper()).postDelayed({ inject() }, 600)
                    }
                }
            }
        }
    }

    private fun inject() {
        if (injected) return
        injected = true
        try {
            val wv = bridge?.webView ?: return
            val js = when {
                showSettings -> "if(typeof openSettings==='function')openSettings();"
                mode == 4 -> "if(typeof showNetPanel==='function')showNetPanel();"
                mode > 0 -> "if(typeof startGame==='function')startGame($mode);"
                else -> ""
            }
            if (js.isNotEmpty()) {
                wv.evaluateJavascript(js, null)
                Handler(Looper.getMainLooper()).postDelayed({
                    wv.evaluateJavascript(js, null)
                }, 800)
            }
        } catch (e: Exception) {}
    }
}
