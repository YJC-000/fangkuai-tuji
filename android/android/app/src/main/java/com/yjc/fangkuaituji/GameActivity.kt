package com.yjc.fangkuaituji

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import com.getcapacitor.BridgeActivity

class GameActivity : BridgeActivity() {

    private var mode = 1
    private var showSettings = false
    private var injected = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = intent.getIntExtra("mode", 1)
        showSettings = intent.getBooleanExtra("showSettings", false)

        bridge?.webView?.let { wv ->
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
        }

        scheduleInject(800)
    }

    private fun scheduleInject(delay: Long) {
        handler.postDelayed({ tryInject() }, delay)
    }

    private fun tryInject() {
        if (injected) return
        try {
            val wv = bridge?.webView
            if (wv == null) {
                scheduleInject(500)
                return
            }
            val js = when {
                showSettings -> "if(typeof openSettings==='function'){openSettings();true}else{false}"
                mode == 4 -> "if(typeof showNetPanel==='function'){showNetPanel();true}else{false}"
                mode > 0 -> "if(typeof startGame==='function'){startGame($mode);true}else{false}"
                else -> "true"
            }
            wv.evaluateJavascript(js) { result ->
                if (result == "true") {
                    injected = true
                } else {
                    scheduleInject(500)
                }
            }
        } catch (e: Exception) {
            scheduleInject(500)
        }
    }
}
