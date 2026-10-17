package com.yjc.fangkuaituji

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val bgTop = Color.parseColor("#1A1612")
    private val bgBottom = Color.parseColor("#241E17")
    private val wave1 = Color.parseColor("#E8A04A")
    private val wave2 = Color.parseColor("#8BB876")
    private val wave3 = Color.parseColor("#D4A574")
    private val cardBg = Color.parseColor("#2A241D")
    private val cardBorder = Color.parseColor("#3D3428")
    private val textPrimary = Color.parseColor("#F5EDE0")
    private val textSecondary = Color.parseColor("#B0A498")
    private val textMuted = Color.parseColor("#6B6158")

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
    ).toInt()

    inner class WaveView(context: android.content.Context) : View(context) {
        private val p1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 20 }
        private val p2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 15 }
        private val p3 = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 12 }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            canvas.drawRect(0f, 0f, w, h, Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, h, bgTop, bgBottom, Shader.TileMode.CLAMP)
            })
            wave(canvas, w, h, h * 0.52f, h * 0.13f, w * 0.32f, p1, wave1)
            wave(canvas, w, h, h * 0.62f, h * 0.10f, w * 0.27f, p2, wave2)
            wave(canvas, w, h, h * 0.72f, h * 0.08f, w * 0.38f, p3, wave3)
        }

        private fun wave(c: Canvas, w: Float, h: Float, baseY: Float, amp: Float, period: Float, p: Paint, color: Int) {
            p.color = color
            val path = Path()
            path.moveTo(0f, baseY)
            var x = 0f
            while (x < w) {
                path.cubicTo(x + period * .25f, baseY - amp, x + period * .75f, baseY + amp, x + period, baseY)
                x += period
            }
            path.lineTo(w, h); path.lineTo(0f, h); path.close()
            c.drawPath(path, p)
        }
    }

    private fun cardBg() = GradientDrawable().apply {
        setColor(cardBg); cornerRadius = dp(28).toFloat(); setStroke(dp(1), cardBorder)
    }

    private fun iconBg(c: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL; setColor(c)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        val frame = FrameLayout(this)
        frame.addView(WaveView(this), FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))

        val ui = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(36), dp(32), dp(24))
        }

        ui.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
            addView(TextView(this@MainActivity).apply {
                text = "方块突击"; setTextColor(textPrimary); textSize = 34f
                typeface = Typeface.DEFAULT_BOLD; letterSpacing = .05f
            })
            addView(TextView(this@MainActivity).apply {
                text = "BLOCK ASSAULT · 3D FPS"; setTextColor(textMuted); textSize = 12f
                setPadding(0, dp(6), 0, 0); letterSpacing = .15f
            })
        })

        val cards = mutableListOf<View>()
        fun mc(icon: String, name: String, desc: String, color: Int, mode: Int, isNet: Boolean = false) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(18), dp(18), dp(18)); background = cardBg()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dp(12)) }
                isClickable = true; elevation = dp(4).toFloat()
                setOnClickListener {
                    if (isNet) {
                        startActivity(Intent(this@MainActivity, NetActivity::class.java))
                    } else {
                        val i = Intent(this@MainActivity, GameActivity::class.java); i.putExtra("mode", mode); startActivity(i)
                    }
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            }
            card.addView(TextView(this).apply {
                text = icon; textSize = 20f; gravity = Gravity.CENTER; background = iconBg(color)
                setTextColor(Color.parseColor("#1A1612")); typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { setMargins(0, 0, dp(14), 0) }
            })
            card.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@MainActivity).apply {
                    text = name; setTextColor(textPrimary); textSize = 16f; typeface = Typeface.DEFAULT_BOLD
                })
                addView(TextView(this@MainActivity).apply {
                    text = desc; setTextColor(textSecondary); textSize = 11f; setPadding(0, dp(2), 0, 0)
                })
            })
            card.addView(TextView(this).apply {
                text = "›"; setTextColor(textMuted); textSize = 24f; typeface = Typeface.DEFAULT_BOLD
            })
            cards.add(card)
            ui.addView(card)
        }
        mc("⚔", "生存模式", "消灭敌人 · 存活到底", wave1, 1)
        mc("☠", "团队死斗", "限时击杀 · 比拼得分", Color.parseColor("#C77D5E"), 2)
        mc("∞", "无尽狂潮", "无限敌人 · 挑战极限", wave2, 5)
        mc("◎", "联机模式", "P2P 直连 · 合作对战", wave3, 4, isNet = true)

        ui.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })

        ui.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            addView(TextView(this@MainActivity).apply {
                text = "  ⚙  设置  "; setTextColor(textSecondary); textSize = 13f
                setPadding(dp(28), dp(10), dp(28), dp(10))
                background = GradientDrawable().apply {
                    setColor(cardBg); cornerRadius = dp(24).toFloat(); setStroke(dp(1), cardBorder)
                }
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            })
        })
        ui.addView(TextView(this).apply {
            text = "V3.3.0"; setTextColor(textMuted); textSize = 10f
            gravity = Gravity.CENTER; setPadding(0, dp(10), 0, 0)
        })

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(ui, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))
        }
        frame.addView(scroll, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(frame)

        ui.translationY = dp(40).toFloat(); ui.alpha = 0f
        ui.animate().translationY(0f).alpha(1f).setDuration(500)
            .setInterpolator(DecelerateInterpolator(1.5f)).start()
        cards.forEachIndexed { i, c ->
            c.translationY = dp(30).toFloat(); c.alpha = 0f
            c.animate().translationY(0f).alpha(1f).setStartDelay(150L + i * 100L)
                .setDuration(400).setInterpolator(DecelerateInterpolator(1.5f)).start()
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }
}
