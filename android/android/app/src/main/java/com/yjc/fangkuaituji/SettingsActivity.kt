package com.yjc.fangkuaituji

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

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
            layoutParams = ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT)
        }

        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(20))
        }
        val backBtn = makeButton("← 返回", "#3A3228", "#D4A574", 14f, dp(12)) {
            finish(); overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }.apply { layoutParams = LinearLayout.LayoutParams(dp(80), dp(40)) }
        val title = TextView(this).apply {
            text = "设置"
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
        root.addView(makeSlider("鼠标灵敏度", "mouse_sens", 0.2f, 3.0f, 1.0f))
        root.addView(makeSlider("触摸灵敏度", "touch_sens", 0.2f, 3.0f, 1.0f))
        root.addView(makeSlider("手柄视角灵敏度", "gamepad_sens", 0.2f, 3.0f, 1.0f))

        root.addView(makeSectionTitle("天气"))
        root.addView(makeWeatherPicker())

        root.addView(makeSectionTitle("画质"))
        root.addView(makeQualityPicker())

        root.addView(makeSectionTitle("音量"))
        root.addView(makeSlider("主音量", "volume", 0f, 1.0f, 0.8f))

        root.addView(makeSectionTitle("关于"))
        val about = TextView(this).apply {
            text = "方块突击 V3.3.0\nThree.js 3D 第一人称射击\n支持手柄 / 键盘 / 触摸操作"
            textSize = 12f
            setTextColor(Color.parseColor("#6B6158"))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(20))
        }
        root.addView(about)

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun makeSectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.parseColor("#E8A04A"))
            setPadding(0, dp(16), 0, dp(8))
        }
    }

    private fun makeSlider(label: String, key: String, min: Float, max: Float, def: Float): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg()
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val labelTv = TextView(this).apply {
            this.text = label
            textSize = 14f
            setTextColor(Color.parseColor("#F5EDE0"))
            layoutParams = LinearLayout.LayoutParams(0, dp(30), 1f)
        }
        val valTv = TextView(this).apply {
            text = String.format("%.2fx", prefs.getFloat(key, def))
            textSize = 13f
            setTextColor(Color.parseColor("#E8A04A"))
        }
        row.addView(labelTv)
        row.addView(valTv)
        val seek = SeekBar(this).apply {
            max = ((max - min) * 100).toInt()
            progress = ((prefs.getFloat(key, def) - min) * 100).toInt()
            setPadding(0, dp(4), 0, 0)
        }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                val v = min + p / 100f
                valTv.text = String.format("%.2fx", v)
                prefs.edit().putFloat(key, v).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        layout.addView(row)
        layout.addView(seek)
        return layout
    }

    private fun makeWeatherPicker(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg()
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val options = arrayOf("默认", "早晨", "夜晚", "永久早晨", "永久夜晚")
        val keys = arrayOf("default", "morning", "night", "perm_morning", "perm_night")
        val current = prefs.getString("weather", "default") ?: "default"
        val grid = GridLayout(this).apply {
            columnCount = 3
            setPadding(0, dp(8), 0, 0)
        }
        options.forEachIndexed { i, opt ->
            val btn = makeButton(opt,
                if (current == keys[i]) "#E8A04A" else "#3A3228",
                if (current == keys[i]) "#1A1714" else "#D4A574",
                12f, dp(10)) {
                prefs.edit().putString("weather", keys[i]).apply()
                recreate()
            }.apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = dp(90); height = dp(38)
                    setMargins(dp(2), dp(4), dp(2), dp(4))
                }
            }
            grid.addView(btn)
        }
        layout.addView(grid)
        return layout
    }

    private fun makeQualityPicker(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg()
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val options = arrayOf("流畅", "均衡", "高清")
        val keys = arrayOf("low", "medium", "high")
        val current = prefs.getString("quality", "medium") ?: "medium"
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        options.forEachIndexed { i, opt ->
            val btn = makeButton(opt,
                if (current == keys[i]) "#8BB876" else "#3A3228",
                if (current == keys[i]) "#1A1714" else "#D4A574",
                13f, dp(10)) {
                prefs.edit().putString("quality", keys[i]).apply()
                recreate()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
            }
            row.addView(btn)
        }
        layout.addView(row)
        return layout
    }

    private fun makeButton(text: String, bgColor: String, textColor: String, textSize: Float, radiusDp: Int): Button {
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
