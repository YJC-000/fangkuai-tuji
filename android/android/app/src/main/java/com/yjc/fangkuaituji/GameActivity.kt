package com.yjc.fangkuaituji

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.getcapacitor.BridgeActivity

class GameActivity : BridgeActivity() {

    private lateinit var loadingView: View
    private val handler = Handler(Looper.getMainLooper())
    private var injected = false
    private var retryCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        loadingView = createLoadingView()
        addContentView(loadingView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

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

        scheduleInject(1000)
    }

    private fun createLoadingView(): View {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1A1714"))
        }
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER)
        }
        val title = TextView(this).apply {
            text = "方块突击"
            textSize = 30f
            setTextColor(Color.parseColor("#E8A04A"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        val subtitle = TextView(this).apply {
            text = "正在加载战场…"
            textSize = 13f
            setTextColor(Color.parseColor("#B0A498"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 28)
        }
        val bar = ProgressBar(this).apply { isIndeterminate = true }
        inner.addView(title)
        inner.addView(subtitle)
        inner.addView(bar)
        root.addView(inner)
        return root
    }

    private fun scheduleInject(delay: Long) {
        handler.postDelayed({ tryInject() }, delay)
    }

    private fun tryInject() {
        if (injected) return
        retryCount++
        try {
            val wv = bridge?.webView
            if (wv == null) { scheduleInject(500); return }

            val mode = intent.getIntExtra("mode", -1)
            val isNet = intent.getBooleanExtra("isNet", false)
            val create = intent.getBooleanExtra("create", true)
            val coop = intent.getBooleanExtra("coop", true)
            val room = intent.getStringExtra("room") ?: ""
            val pwd = intent.getStringExtra("pwd") ?: ""
            val name = intent.getStringExtra("name") ?: ""

            val hideJs = """
                (function(){
                    try{
                        document.querySelectorAll('.overlay').forEach(function(o){o.classList.add('hidden');});
                        var m=document.getElementById('menu');if(m){m.style.display='none';m.classList.add('hidden');}
                        var s=document.getElementById('settings');if(s)s.classList.add('hidden');
                        var np=document.getElementById('netpanel');if(np)np.classList.add('hidden');
                        return true;
                    }catch(e){return false;}
                })();
            """.trimIndent()

            wv.evaluateJavascript(hideJs) { r1 ->
                val prefs = getSharedPreferences("fk_settings", MODE_PRIVATE)
                val ms = prefs.getFloat("mouse_sens", 1.0f)
                val ts = prefs.getFloat("touch_sens", 1.0f)
                val gs = prefs.getFloat("gamepad_sens", 1.0f)
                val weather = prefs.getString("weather", "default") ?: "default"

                val settingsJs = """
                    (function(){
                        try{
                            if(typeof settings!=='undefined'){
                                settings.mouseMul=$ms;settings.touchMul=$ts;settings.gamepadLookMul=$gs;
                            }
                            return true;
                        }catch(e){return false;}
                    })();
                """.trimIndent()
                wv.evaluateJavascript(settingsJs, null)

                val actionJs = when {
                    isNet -> """
                        (function(){
                            try{
                                var n=document.getElementById('netname');if(n)n.value='$name';
                                var r=document.getElementById('netroom');if(r)r.value='$room';
                                var p=document.getElementById('netpass');if(p)p.value='$pwd';
                                if(typeof doConnect==='function'){doConnect($create,$coop);return true;}
                                return false;
                            }catch(e){return false;}
                        })();
                    """.trimIndent()
                    mode == 4 -> """
                        (function(){try{if(typeof showNetPanel==='function'){showNetPanel();return true;}return false;}catch(e){return false;}})();
                    """.trimIndent()
                    mode > 0 -> """
                        (function(){try{if(typeof startGame==='function'){startGame($mode);return true;}return false;}catch(e){return false;}})();
                    """.trimIndent()
                    else -> "true"
                }

                wv.evaluateJavascript(actionJs) { r2 ->
                    if (r2 == "true" || retryCount > 20) {
                        injected = true
                        if (weather != "default") {
                            val wCode = when (weather) {
                                "morning" -> 0; "night" -> 1
                                "perm_morning" -> 2; "perm_night" -> 3
                                else -> -1
                            }
                            if (wCode >= 0) {
                                handler.postDelayed({
                                    wv.evaluateJavascript(
                                        "(function(){try{if(typeof setWeather==='function'){setWeather($wCode);}}catch(e){}})();", null)
                                }, 1500)
                            }
                        }
                        handler.postDelayed({
                            loadingView.animate().alpha(0f).setDuration(500).withEndAction {
                                loadingView.visibility = View.GONE
                            }.start()
                        }, 800)
                    } else {
                        scheduleInject(500)
                    }
                }
            }
        } catch (e: Exception) {
            if (retryCount < 30) scheduleInject(500)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }
}
