package com.yjc.fangkuaituji

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)

        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(80, 0, 80, 0)
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#0D1B2A"), Color.parseColor("#1B2838"), Color.parseColor("#0D1B2A"))
            )
        }

        val title = TextView(this).apply {
            text = "方块突击"
            textSize = 42f
            setTextColor(Color.parseColor("#4FC3F7"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 120, 0, 8)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "3D 第一人称射击"
            textSize = 16f
            setTextColor(Color.parseColor("#8AA4B8"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 60)
        }
        root.addView(subtitle)

        fun makeButton(text: String, color: Int): Button {
            val btn = Button(this)
            btn.text = text
            btn.textSize = 18f
            btn.setTextColor(Color.WHITE)
            btn.setAllCaps(false)
            val bg = GradientDrawable().apply {
                setColor(color)
                cornerRadius = 40f
            }
            btn.background = bg
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 20, 0, 20)
            btn.layoutParams = params
            btn.setPadding(0, 35, 0, 35)
            return btn
        }

        val btnSurvival = makeButton("🔫 生存模式", Color.parseColor("#1565C0"))
        btnSurvival.setOnClickListener { startGame(1) }
        root.addView(btnSurvival)

        val btnEndless = makeButton("🌊 无尽狂潮", Color.parseColor("#2E7D32"))
        btnEndless.setOnClickListener { startGame(5) }
        root.addView(btnEndless)

        val btnMulti = makeButton("🌐 联机模式", Color.parseColor("#6A1B9A"))
        btnMulti.setOnClickListener { startGame(4) }
        root.addView(btnMulti)

        val btnSettings = makeButton("⚙️ 设置", Color.parseColor("#455A64"))
        btnSettings.setOnClickListener {
            val i = Intent(this, GameActivity::class.java)
            i.putExtra("mode", 0)
            i.putExtra("showSettings", true)
            startActivity(i)
        }
        root.addView(btnSettings)

        val version = TextView(this).apply {
            text = "V3.2.0 · Kotlin UI"
            textSize = 12f
            setTextColor(Color.parseColor("#546E7A"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 60, 0, 0)
        }
        root.addView(version)

        setContentView(root)
    }

    private fun startGame(mode: Int) {
        val i = Intent(this, GameActivity::class.java)
        i.putExtra("mode", mode)
        startActivity(i)
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
