package com.yjc.fangkuaituji

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.getcapacitor.BridgeActivity

class GameActivity : BridgeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getIntExtra("mode", 1)
        val showSettings = intent.getBooleanExtra("showSettings", false)

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val wv = bridge?.webView
                if (wv != null) {
                    val js = when {
                        showSettings -> "if(typeof openSettings==='function')openSettings();"
                        mode == 4 -> "if(typeof showNetPanel==='function')showNetPanel();"
                        mode > 0 -> "if(typeof startGame==='function')startGame($mode);"
                        else -> ""
                    }
                    if (js.isNotEmpty()) {
                        wv.evaluateJavascript(js, null)
                    }
                }
            } catch (e: Exception) {
            }
        }, 1000)
    }
}
