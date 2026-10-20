package com.yjc.fangkuaituji

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
import android.widget.Toast
import com.getcapacitor.BridgeActivity
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GameActivity : BridgeActivity() {

    private lateinit var loadingView: View
    private val handler = Handler(Looper.getMainLooper())
    private var booted = false
    private var bootTries = 0
    private var lastBackTime = 0L

    // 意图参数
    private var isNet = false
    private var create = true
    private var mode = 1
    private var room = ""
    private var pwd = ""
    private var name = ""

    companion object {
        private const val ROOM_API = "https://crudcrud.com/api/77ffb1c1c7424d8a9eeb5ed8e590fdab"
        // Capacitor 5 本地资源服务器（默认 https://localhost）
        private const val LOCAL_BASE = "https://localhost/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)

        loadingView = createLoadingView()
        addContentView(loadingView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // 读取意图
        isNet = intent.getBooleanExtra("isNet", false)
        create = intent.getBooleanExtra("create", true)
        val coop = intent.getBooleanExtra("coop", true)
        mode = intent.getIntExtra("mode", 1)
        room = intent.getStringExtra("room") ?: ""
        pwd = intent.getStringExtra("pwd") ?: ""
        name = intent.getStringExtra("name") ?: ""
        if (isNet) mode = if (coop) 4 else 6

        bridge?.webView?.let { configureWebView(it) }
        scheduleBoot(300)
    }

    private fun configureWebView(wv: WebView) {
        val s = wv.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.setSupportZoom(false)
        s.builtInZoomControls = false
        s.displayZoomControls = false
        // 【V3.4.2 修复模糊】不要用 overview 缩放整页；按设备像素 1:1 渲染
        s.loadWithOverviewMode = false
        s.useWideViewPort = true
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.mediaPlaybackRequiresUserGesture = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        wv.setInitialScale(100)
        wv.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        wv.isVerticalScrollBarEnabled = false
        wv.isHorizontalScrollBarEnabled = false
    }

    private fun createLoadingView(): View {
        val root = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#1A1612")) }
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        }
        inner.addView(TextView(this).apply {
            text = "方块突击"; textSize = 30f
            setTextColor(Color.parseColor("#E8A04A")); gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        })
        inner.addView(TextView(this).apply {
            text = "正在加载战场…"; textSize = 13f
            setTextColor(Color.parseColor("#B0A498")); gravity = Gravity.CENTER
            setPadding(0, 0, 0, 28)
        })
        inner.addView(ProgressBar(this).apply { isIndeterminate = true })
        root.addView(inner)
        return root
    }

    private fun scheduleBoot(delay: Long) {
        handler.postDelayed({ tryBoot() }, delay)
    }

    private fun tryBoot() {
        if (booted) return
        val wv = bridge?.webView
        if (wv == null) { scheduleBoot(300); return }
        try {
            configureWebView(wv)
            val prefs = getSharedPreferences("fk_settings", MODE_PRIVATE)
            val ms = prefs.getFloat("mouse_sens", 1.0f)
            val ts = prefs.getFloat("touch_sens", 1.0f)
            val gs = prefs.getFloat("gamepad_sens", 1.0f)
            val vol = prefs.getFloat("volume", 0.8f)
            val quality = prefs.getString("quality", "medium") ?: "medium"
            val weather = prefs.getString("weather", "default") ?: "default"

            // 用 URL 参数让游戏自身 autostart（与电脑版/浏览器一致，避免注入时机导致的建房卡死）
            val sb = StringBuilder(LOCAL_BASE + "index.html?autostart=1&touch=1")
            sb.append("&mode=").append(mode)
            sb.append("&sens=").append(ms).append("&tsens=").append(ts).append("&psens=").append(gs)
            sb.append("&vol=").append(vol).append("&quality=").append(quality)
            if (weather != "default") sb.append("&weather=").append(weather)
            if (isNet) {
                sb.append("&p2p=1")
                sb.append("&create=").append(if (create) "1" else "0")
                sb.append("&room=").append(enc(room))
                if (pwd.isNotEmpty()) sb.append("&pwd=").append(enc(pwd))
                if (name.isNotEmpty()) sb.append("&name=").append(enc(name))
            }
            wv.loadUrl(sb.toString())
            booted = true

            // 注入设置（灵敏度/音量/画质，实时生效）
            val settingsJs = """
                (function(){try{
                  if(typeof settings!=='undefined'){
                    settings.mouseMul=$ms;settings.touchMul=$ts;settings.gamepadLookMul=$gs;
                    settings.volume=$vol;settings.quality='$quality';
                  }
                }catch(e){}})();
            """.trimIndent()
            handler.postDelayed({ wv.evaluateJavascript(settingsJs, null) }, 1200)
            handler.postDelayed({ wv.evaluateJavascript(settingsJs, null) }, 2600)

            // 轮询等待游戏画面（单机 canvas / 联机等待面板）出现后淡出 loading
            scheduleFadeCheck()
        } catch (e: Exception) {
            bootTries++
            if (bootTries < 20) scheduleBoot(500)
        }
    }

    private fun scheduleFadeCheck() {
        handler.postDelayed({
            val wv = bridge?.webView ?: run { handler.postDelayed({ scheduleFadeCheck() }, 500); return@postDelayed }
            wv.evaluateJavascript(
                "(function(){try{" +
                "var c=document.querySelector('canvas');" +
                "var np=document.getElementById('netpanel');" +
                "if(c)return 'canvas';" +
                "if(np&&!np.classList.contains('hidden'))return 'net';" +
                "return 'none';}catch(e){return 'none';}})();"
            ) { r ->
                if (r != null && r.contains("canvas") || r != null && r.contains("net")) {
                    fadeOutLoading()
                } else {
                    bootTries++
                    if (bootTries < 30) scheduleFadeCheck() else fadeOutLoading()
                }
            }
        }, 500)
    }

    private fun fadeOutLoading() {
        if (loadingView.visibility == View.GONE) return
        loadingView.animate().alpha(0f).setDuration(450).withEndAction {
            loadingView.visibility = View.GONE
        }.start()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        val wv = bridge?.webView
        if (wv != null) {
            // 第一次：暂停游戏（若正在玩）；第二次：返回主菜单
            wv.evaluateJavascript(
                "(function(){try{var p=document.getElementById('pause');" +
                "if(p&&p.classList.contains('hidden')&&(typeof gameState!=='undefined')&&gameState==='playing'){" +
                "paused=true;firing=false;mouseFiring=false;p.classList.remove('hidden');return 'paused';}" +
                "return 'ready';}catch(e){return 'ready';}})();"
            ) { r ->
                if (r != null && r.contains("paused") && now - lastBackTime > 600) {
                    lastBackTime = now
                    Toast.makeText(this, "再按一次返回主菜单", Toast.LENGTH_SHORT).show()
                } else {
                    exitToMenu()
                }
            }
        } else {
            exitToMenu()
        }
    }

    private fun exitToMenu() {
        val wv = bridge?.webView
        // 通知游戏清理（关闭 Peer / 注销房间）
        try {
            wv?.evaluateJavascript(
                "(function(){try{if(typeof p2pClose==='function')p2pClose();if(typeof unregisterRoom==='function')unregisterRoom();}catch(e){}})();",
                null)
        } catch (_: Exception) {}
        // 主机原生侧兜底注销房间
        if (isNet && create && room.isNotEmpty()) {
            val rm = room
            Thread {
                try {
                    val conn = URL("$ROOM_API/rooms").openConnection() as HttpURLConnection
                    conn.connectTimeout = 4000; conn.readTimeout = 4000
                    val br = BufferedReader(InputStreamReader(conn.inputStream))
                    val sb = StringBuilder(); var ln: String?
                    while (br.readLine().also { ln = it } != null) sb.append(ln)
                    br.close(); conn.disconnect()
                    val arr = JSONArray(sb.toString())
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        if (o.optString("roomId") == rm) {
                            val id = o.optString("_id")
                            if (id.isNotEmpty()) {
                                val dc = URL("$ROOM_API/rooms/$id").openConnection() as HttpURLConnection
                                dc.requestMethod = "DELETE"; dc.connectTimeout = 4000; dc.readTimeout = 4000
                                dc.responseCode; dc.disconnect()
                            }
                        }
                    }
                } catch (_: Exception) {}
            }.start()
        }
        // 重置 WebView，停止 WebGL/动画/WebRTC，避免返回主菜单后卡死
        try { wv?.loadUrl("about:blank") } catch (_: Exception) {}
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun enc(s: String): String =
        java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }
}
