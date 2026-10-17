package com.yjc.fangkuaituji

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue

class MainActivity : AppCompatActivity() {

    private val bgGrad = intArrayOf(
        Color.parseColor("#1F1B16"),
        Color.parseColor("#1A1714"),
        Color.parseColor("#201B16")
    )
    private val cardColor = Color.parseColor("#2A241D")
    private val cardBorder = Color.parseColor("#3A3228")
    private val accentOrange = Color.parseColor("#E8A04A")
    private val accentGreen = Color.parseColor("#8BB876")
    private val accentAmber = Color.parseColor("#D4A574")
    private val textPrimary = Color.parseColor("#F5EDE0")
    private val textSecondary = Color.parseColor("#B0A498")
    private val textMuted = Color.parseColor("#6B6158")

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
    ).toInt()

    private fun cardBg(): GradientDrawable = GradientDrawable().apply {
        setColor(cardColor)
        cornerRadius = dp(24).toFloat()
        setStroke(1, cardBorder)
    }

    private fun circleBg(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun pillBg(color: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(30).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, bgGrad)
            setPadding(dp(24), dp(40), dp(24), dp(20))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), 0, dp(4), dp(28))
        }
        header.addView(TextView(this).apply {
            text = "方块突击"
            setTextColor(textPrimary)
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
        })
        header.addView(TextView(this).apply {
            text = "Block Assault · 3D FPS"
            setTextColor(textMuted)
            textSize = 13f
            setPadding(0, dp(4), 0, 0)
        })
        root.addView(header)

        fun modeCard(icon: String, name: String, desc: String, accent: Int, mode: Int) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(20), dp(20), dp(20), dp(20))
                background = cardBg()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dp(14)) }
                isClickable = true
                setOnClickListener { startGame(mode) }
            }
            card.addView(TextView(this).apply {
                text = icon
                textSize = 22f
                gravity = Gravity.CENTER
                background = circleBg(accent)
                setTextColor(Color.parseColor("#1A1714"))
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)).apply {
                    setMargins(0, 0, dp(16), 0)
                }
            })
            val texts = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            texts.addView(TextView(this).apply {
                text = name
                setTextColor(textPrimary)
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
            })
            texts.addView(TextView(this).apply {
                text = desc
                setTextColor(textSecondary)
                textSize = 12f
                setPadding(0, dp(3), 0, 0)
            })
            card.addView(texts)
            card.addView(TextView(this).apply {
                text = "→"
                setTextColor(textMuted)
                textSize = 20f
            })
            root.addView(card)
        }

        modeCard("⚔", "生存模式", "消灭敌人，存活到底", accentOrange, 1)
        modeCard("~", "无尽狂潮", "无限敌人，挑战极限", accentGreen, 5)
        modeCard("◎", "联机模式", "P2P 直连，合作对战", accentAmber, 4)

        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })

        val settingsWrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(20))
        }
        settingsWrap.addView(TextView(this).apply {
            text = "⚙  设置"
            setTextColor(textSecondary)
            textSize = 14f
            setPadding(dp(32), dp(12), dp(32), dp(12))
            background = pillBg(cardColor)
            setOnClickListener {
                val i = Intent(this@MainActivity, GameActivity::class.java)
                i.putExtra("mode", 0)
                i.putExtra("showSettings", true)
                startActivity(i)
            }
        })
        root.addView(settingsWrap)

        root.addView(TextView(this).apply {
            text = "V3.2.0"
            setTextColor(textMuted)
            textSize = 11f
            gravity = Gravity.CENTER
        })

        setContentView(root)
    }

    private fun startGame(mode: Int) {
        val i = Intent(this, GameActivity::class.java)
        i.putExtra("mode", mode)
        startActivity(i)
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
}
