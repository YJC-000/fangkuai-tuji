package com.yjc.fangkuaituji

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private val weatherButtons = mutableListOf<Button>()
    private val weatherVals = listOf("default", "morning", "night", "perm_morning", "perm_night")
    private val qualityButtons = mutableListOf<Button>()
    private val qualityVals = listOf("low", "medium", "high")

    private val C_BG = "#1A1612"
    private val C_CARD = "#2A241D"
    private val C_STROKE = "#3D3428"
    private val C_INK = "#F5EDE0"
    private val C_INK2 = "#B0A498"
    private val C_MUTED = "#8A7F72"
    private val C_ORANGE = "#E8A04A"
    private val C_GREEN = "#8BB876"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        immersive()

        prefs = getSharedPreferences("fk_settings", MODE_PRIVATE)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor(C_BG))
            isVerticalScrollBarEnabled = false
            setPadding(dp(20), dp(16), dp(20), dp(40))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // 标题栏
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(12))
        }
        val backBtn = makeButton("← 返回", "#3A3228", C_ORANGE, 14f, 12) {
            finish(); overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }.apply { layoutParams = LinearLayout.LayoutParams(dp(84), dp(42)) }
        titleBar.addView(backBtn)
        titleBar.addView(TextView(this).apply {
            text = "游戏设置"; textSize = 22f; setTextColor(Color.parseColor(C_INK))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f)
        })
        titleBar.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(84), dp(42)) })
        root.addView(titleBar)

        // 灵敏度
        root.addView(section("灵敏度"))
        root.addView(makeSlider("鼠标灵敏度", "mouse_sens", 0.3f, 3f, 1f))
        root.addView(makeSlider("触摸视角灵敏度", "touch_sens", 0.3f, 3f, 1f))
        root.addView(makeSlider("手柄视角灵敏度", "gamepad_sens", 0.3f, 3f, 1f))

        // 天气
        root.addView(section("天气"))
        val weatherCard = card()
        val weatherLabels = listOf("默认", "早晨", "夜晚", "永久早晨", "永久夜晚")
        val wr1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val wr2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        weatherLabels.forEachIndexed { i, label ->
            val b = makeButton(label, "#3A3228", C_ORANGE, 12f, 12) {
                prefs.edit().putString("weather", weatherVals[i]).apply()
                updateWeatherButtons()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
            }
            weatherButtons.add(b)
            if (i < 3) wr1.addView(b) else wr2.addView(b)
        }
        weatherCard.addView(wr1); weatherCard.addView(wr2)
        root.addView(weatherCard)

        // 画质
        root.addView(section("画质"))
        val qualityCard = card()
        val qRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("流畅", "均衡", "高清").forEachIndexed { i, label ->
            val b = makeButton(label, "#3A3228", C_ORANGE, 13f, 14) {
                prefs.edit().putString("quality", qualityVals[i]).apply()
                updateQualityButtons()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(4), 0, dp(4), 0) }
            }
            qualityButtons.add(b); qRow.addView(b)
        }
        qualityCard.addView(qRow)
        root.addView(qualityCard)

        // 音量
        root.addView(section("音量"))
        root.addView(makeSlider("主音量", "volume", 0f, 1f, 0.8f))

        // 关于
        root.addView(section("关于"))
        val about = card()
        about.addView(TextView(this).apply {
            text = "方块突击 V3.4.2"; textSize = 15f; setTextColor(Color.parseColor(C_INK))
            setPadding(0, dp(2), 0, dp(4))
        })
        about.addView(TextView(this).apply {
            text = "3D 第一人称射击 · WebRTC P2P 远程联机"; textSize = 11f
            setTextColor(Color.parseColor(C_INK2)); setPadding(0, dp(2), 0, dp(2))
        })
        about.addView(TextView(this).apply {
            text = "手柄按键与校准请在游戏内暂停 → 设置中调整"; textSize = 11f
            setTextColor(Color.parseColor(C_MUTED)); setPadding(0, dp(6), 0, dp(2))
        })
        root.addView(about)

        scroll.addView(root)
        setContentView(scroll)
        updateWeatherButtons()
        updateQualityButtons()
    }

    private fun updateWeatherButtons() {
        val cur = prefs.getString("weather", "default")
        weatherButtons.forEachIndexed { i, b ->
            val on = weatherVals[i] == cur
            b.background = rounded(if (on) C_ORANGE else "#3A3228", 12)
            b.setTextColor(Color.parseColor(if (on) C_BG else C_ORANGE))
        }
    }

    private fun updateQualityButtons() {
        val cur = prefs.getString("quality", "medium")
        qualityButtons.forEachIndexed { i, b ->
            val on = qualityVals[i] == cur
            b.background = rounded(if (on) C_GREEN else "#3A3228", 14)
            b.setTextColor(Color.parseColor(if (on) C_BG else C_ORANGE))
        }
    }

    private fun makeSlider(label: String, key: String, minVal: Float, maxVal: Float, def: Float): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = cardBg()
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(4), 0, dp(4)) }
        }
        val labelRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val labelTv = TextView(this).apply {
            text = label; textSize = 13f; setTextColor(Color.parseColor(C_INK))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val valTv = TextView(this).apply {
            text = String.format("%.2f", prefs.getFloat(key, def)); textSize = 13f
            setTextColor(Color.parseColor(C_ORANGE))
        }
        labelRow.addView(labelTv); labelRow.addView(valTv)
        val seek = SeekBar(this).apply {
            max = ((maxVal - minVal) * 100).toInt()
            progress = ((prefs.getFloat(key, def) - minVal) * 100).toInt()
            setPadding(0, dp(4), 0, 0)
        }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                val v = minVal + p / 100f
                prefs.edit().putFloat(key, v).apply()
                valTv.text = String.format("%.2f", v)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        row.addView(labelRow); row.addView(seek)
        return row
    }

    private fun section(t: String) = TextView(this).apply {
        text = t; textSize = 15f; setTextColor(Color.parseColor(C_ORANGE))
        setPadding(0, dp(16), 0, dp(8))
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; background = cardBg()
        setPadding(dp(16), dp(16), dp(16), dp(16))
    }

    private fun cardBg(): GradientDrawable = rounded(C_CARD, 18).apply { setStroke(dp(1), Color.parseColor(C_STROKE)) }
    private fun rounded(color: String, radiusDp: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(color)); cornerRadius = dp(radiusDp).toFloat()
    }

    private fun makeButton(text: String, bgColor: String, textColor: String, textSize: Float, radiusDp: Int,
                           onClick: (() -> Unit)? = null): Button = Button(this).apply {
        this.text = text; this.textSize = textSize
        setTextColor(Color.parseColor(textColor)); background = rounded(bgColor, radiusDp)
        setPadding(dp(8), 0, dp(8), 0); isAllCaps = false
        stateListAnimator = null
        if (onClick != null) setOnClickListener { onClick() }
    }

    private fun immersive() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus); if (hasFocus) immersive()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
