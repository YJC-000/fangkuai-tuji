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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.parseColor("#0A1628"),
                    Color.parseColor("#0D1F3C"),
                    Color.parseColor("#070D18")
                )
            )
        }

        val glow1 = View(this).apply {
            val d = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(Color.parseColor("#401565C0"), Color.TRANSPARENT)
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = 400f
            }
            background = d
            layoutParams = FrameLayout.LayoutParams(600, 600).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(-150, -150, 0, 0)
            }
        }
        root.addView(glow1)

        val glow2 = View(this).apply {
            val d = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(Color.parseColor("#3000E5FF"), Color.TRANSPARENT)
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = 350f
            }
            background = d
            layoutParams = FrameLayout.LayoutParams(500, 500).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                setMargins(-100, 0, 0, -100)
            }
        }
        root.addView(glow2)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(100, 0, 100, 0)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val titleEn = TextView(this).apply {
            text = "BLOCK ASSAULT"
            textSize = 13f
            setTextColor(Color.parseColor("#4FC3F7"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            letterSpacing = 0.3f
            setPadding(0, 0, 0, 4)
        }
        content.addView(titleEn)

        val titleCn = TextView(this).apply {
            text = "方块突击"
            textSize = 48f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 6)
            setShadowLayer(20f, 0f, 0f, Color.parseColor("#804FC3F7"))
        }
        content.addView(titleCn)

        val titleLine = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#4FC3F7"), Color.TRANSPARENT)
            )
            layoutParams = LinearLayout.LayoutParams(300, 2).apply {
                setMargins(0, 8, 0, 30)
            }
        }
        content.addView(titleLine)

        fun modeCard(icon: String, name: String, desc: String, color: String, mode: Int): LinearLayout {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(30, 22, 30, 22)
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#1A2A42"))
                    cornerRadius = 18f
                    setStroke(1, Color.parseColor("#2A4A6A"))
                }
                background = bg
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 10, 0, 10) }
                setOnClickListener { startGame(mode) }
            }
            val iconCircle = TextView(this).apply {
                text = icon
                textSize = 22f
                gravity = Gravity.CENTER
                val d = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(color))
                }
                background = d
                layoutParams = LinearLayout.LayoutParams(56, 56).apply {
                    setMargins(0, 0, 20, 0)
                }
            }
            card.addView(iconCircle)
            val textCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            textCol.addView(TextView(this).apply {
                text = name
                textSize = 18f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            })
            textCol.addView(TextView(this).apply {
                text = desc
                textSize = 12f
                setTextColor(Color.parseColor("#7A9AB8"))
            })
            card.addView(textCol)
            return card
        }

        content.addView(modeCard("🔫", "生存模式", "消灭敌人，存活到底", "#1565C0", 1))
        content.addView(modeCard("🌊", "无尽狂潮", "敌人无限刷新，挑战极限", "#2E7D32", 5))
        content.addView(modeCard("🌐", "联机模式", "P2P 直连，双人合作/对战", "#6A1B9A", 4))

        content.addView(TextView(this).apply {
            text = "⚙  设置"
            textSize = 15f
            setTextColor(Color.parseColor("#8AA4B8"))
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 0)
            setOnClickListener {
                val i = Intent(this@MainActivity, GameActivity::class.java)
                i.putExtra("mode", 0)
                i.putExtra("showSettings", true)
                startActivity(i)
            }
        })

        content.addView(TextView(this).apply {
            text = "V3.2.0"
            textSize = 11f
            setTextColor(Color.parseColor("#3A5A7A"))
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 0)
        })

        root.addView(content)
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
