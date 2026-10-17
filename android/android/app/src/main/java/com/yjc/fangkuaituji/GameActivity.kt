package com.yjc.fangkuaituji

import android.os.Bundle
import com.getcapacitor.BridgeActivity

class GameActivity : BridgeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getIntExtra("mode", 1)
        val showSettings = intent.getBooleanExtra("showSettings", false)

        this.bridge?.webView?.postDelayed({
            val js = when {
                showSettings -> "if(typeof openSettings==='function')openSettings();"
                mode == 4 -> "if(typeof showNetPanel==='function')showNetPanel();"
                mode > 0 -> "if(typeof startGame==='function')startGame($mode);"
                else -> ""
            }
            if (js.isNotEmpty()) {
                this.bridge?.webView?.evaluateJavascript(js, null)
            }
        }, 800)
    }
}
