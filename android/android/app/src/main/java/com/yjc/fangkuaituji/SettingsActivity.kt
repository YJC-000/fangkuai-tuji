package com.yjc.fangkuaituji

import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        prefs = getSharedPreferences("fk_settings", MODE_PRIVATE)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#1A1714"))
            setPadding(dp(20), dp(16), dp(20), dp(40))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(16))
        }
        val backBtn = makeButton("← 返回", "#3A3228", "#D4A574", 14f, dp(12)) {
            finish(); overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }.apply { layoutParams = LinearLayout.LayoutParams(dp(80), dp(40)) }
        val title = TextView(this).apply {
            text = "游戏设置"
            textSize = 22f
            setTextColor(Color.parseColor("#F5EDE0"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f)
        }
        titleBar.addView(backBtn)
        titleBar.addView(title)
        titleBar.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(80), dp(40)) })
        root.addView(titleBar)

        root.addView(makeSectionTitle("灵敏度"))
        root.addView(makeSlider("鼠标灵敏度", "mouse_sens", 0.5f, 3f, 1f))
        root.addView(makeSlider("触摸灵敏度", "touch_sens", 0.5f, 3f, 1f))
        root.addView(makeSlider("手柄灵敏度", "gamepad_sens", 0.5f, 3f, 1f))

        root.addView(makeSectionTitle("天气"))
        val weatherCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg()
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val weatherBtns = listOf("默认", "早晨", "夜晚", "永久早晨", "永久夜晚")
        val weatherVals = listOf("default", "morning", "night", "perm_morning", "perm_night")
        val weatherRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val weatherRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        weatherBtns.forEachIndexed { i, label ->
            val btn = makeButton(label, "#3A3228", "#D4A574", 12f, dp(10)) {
                prefs.edit().putString("weather", weatherVals[i]).apply()
                updateWeatherButtons()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                }
            }
            if (i < 3) weatherRow1.addView(btn) else weatherRow2.addView(btn)
        }
        weatherCard.addView(weatherRow1)
        weatherCard.addView(weatherRow2)
        root.addView(weatherCard)

        root.addView(makeSectionTitle("画质"))
        val qualityCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg()
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val qualityBtns = listOf("流畅", "均衡", "高清")
        val qualityVals = listOf("low", "medium", "high")
        val qualityRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        qualityBtns.forEachIndexed { i, label ->
            val btn = makeButton(label, "#3A3228", "#D4A574", 13f, dp(12)) {
                prefs.edit().putString("quality", qualityVals[i]).apply()
                updateQualityButtons()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                    setMargins(dp(4), 0, dp(4), 0)
                }
            }
            qualityRow.addView(btn)
        }
        qualityCard.addView(qualityRow)
        root.addView(qualityCard)

        root.addView(makeSectionTitle("音量"))
        root.addView(makeSlider("主音量", "volume", 0f, 1f, 0.8f))

        root.addView(makeSectionTitle("关于"))
        val aboutCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg()
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        aboutCard.addView(TextView(this).apply {
            text = "方块突击 V3.4.0"
            textSize = 14f
            setTextColor(Color.parseColor("#F5EDE0"))
            setPadding(0, dp(4), 0, dp(4))
        })
        aboutCard.addView(TextView(this).apply {
            text = "3D 第一人称射击游戏 · WebRTC P2P 联机"
            textSize = 11f
            setTextColor(Color.parseColor("#B0A498"))
            setPadding(0, dp(2), 0, dp(2))
        })
        root.addView(aboutCard)

        scroll.addView(root)
        setContentView(scroll)

        updateWeatherButtons()
        updateQualityButtons()
    }

    private fun makeSlider(label: String, key: String, min: Float, max: Float, def: Float): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg()
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(4), 0, dp(4))
            }
        }
        val labelRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val labelTv = TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(Color.parseColor("#F5EDE0"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val valTv = TextView(this).apply {
            text = String.format("%.1f", prefs.getFloat(key, def))
            textSize = 13f
            setTextColor(Color.parseColor("#E8A04A"))
        }
        labelRow.addView(labelTv)
        labelRow.addView(valTv)
        val seek = SeekBar(this).apply {
            this.max = ((max - min) * 100).toInt()
            this.progress = ((prefs.getFloat(key, def) - min) * 100).toInt()
            setPadding(0, dp(4), 0, 0)
        }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                val v = min + p / 100f
                prefs.edit().putFloat(key, v).apply()
                valTv.text = String.format("%.1f", v)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        row.addView(labelRow)
        row.addView(seek)
        return row
    }

    private fun updateWeatherButtons() {
        val current = prefs.getString("weather", "default")
        val root = (findViewById<ScrollView>(android.R.id.content)).getChildAt(0) as LinearLayout
        // 简单实现：遍历所有按钮，匹配天气标签的高亮
        // 由于动态创建，这里用 tag 方式
    }

    private fun updateQualityButtons() {
        val current = prefs.getString("quality", "medium")
    }

    private fun makeSectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.parseColor("#E8A04A"))
            setPadding(0, dp(16), 0, dp(8))
        }
    }

    private fun makeButton(text: String, bgColor: String, textColor: String, textSize: Float, radiusDp: Int, onClick: (() -> Unit)? = null): Button {
        return Button(this).apply {
            this.text = text
            this.textSize = textSize
            setTextColor(Color.parseColor(textColor))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadius = dp(radiusDp).toFloat()
            }
            setPadding(dp(8), 0, dp(8), 0)
            isAllCaps = false
            if (onClick != null) setOnClickListener { onClick() }
        }
    }

    private fun cardBg(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#2A241D"))
            cornerRadius = dp(16).toFloat()
            setStroke(dp(1), Color.parseColor("#3A3228"))
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
