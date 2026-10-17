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
import android.widget.LinearLayout
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
        private val paint1 = Paint(Paint.ANTI_ALIAS_FLAG)
        private val paint2 = Paint(Paint.ANTI_ALIAS_FLAG)
        private val paint3 = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            paint1.alpha = 18
            paint2.alpha = 14
            paint3.alpha = 10
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val bg = LinearGradient(0f, 0f, 0f, h, bgTop, bgBottom, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, Paint().apply { shader = bg })
            drawWave(canvas, w, h * 0.55f, h * 0.12f, w * 0.3f, paint1, wave1)
            drawWave(canvas, w, h * 0.65f, h * 0.10f, w * 0.25f, paint2, wave2)
            drawWave(canvas, w, h * 0.75f, h * 0.08f, w * 0.35f, paint3, wave3)
        }

        private fun drawWave(c: Canvas, w: Float, baseY: Float, amp: Float, period: Float, p: Paint, color: Int) {
            p.color = color
            val path = Path()
            path.moveTo(0f, baseY)
            var x = 0f
            while (x < w) {
                path.cubicTo(
                    x + period * 0.25f, baseY - amp,
                    x + period * 0.75f, baseY + amp,
                    x + period, baseY
                )
                x += period
            }
            path.lineTo(w, h.toFloat())
            path.lineTo(0f, h.toFloat())
            path.close()
            c.drawPath(path, p)
        }
    }

    private fun glassCard(): GradientDrawable = GradientDrawable().apply {
        setColor(cardBg)
        cornerRadius = dp(28).toFloat()
        setStroke(dp(1), cardBorder)
    }

    private fun circleIcon(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(36), dp(32), dp(24))
        }

        val wave = WaveView(this)
        root.addView(wave, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val titleWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(24))
        }
        titleWrap.addView(TextView(this).apply {
            text = "方块突击"
            setTextColor(textPrimary)
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.05f
        })
        titleWrap.addView(TextView(this).apply {
            text = "BLOCK ASSAULT · 3D FPS"
            setTextColor(textMuted)
            textSize = 12f
            setPadding(0, dp(6), 0, 0)
            letterSpacing = 0.15f
        })
        content.addView(titleWrap)

        val cards = mutableListOf<View>()
        fun modeCard(icon: String, name: String, desc: String, accent: Int, mode: Int): View {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), dp(18), dp(18), dp(18))
                background = glassCard()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dp(12)) }
                isClickable = true
                elevation = dp(4).toFloat()
                setOnClickListener { startGame(mode) }
            }
            card.addView(TextView(this).apply {
                text = icon
                textSize = 20f
                gravity = Gravity.CENTER
                background = circleIcon(accent)
                setTextColor(Color.parseColor("#1A1612"))
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                    setMargins(0, 0, dp(14), 0)
                }
            })
            val texts = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            texts.addView(TextView(this).apply {
                text = name
                setTextColor(textPrimary)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
            })
            texts.addView(TextView(this).apply {
                text = desc
                setTextColor(textSecondary)
                textSize = 11f
                setPadding(0, dp(2), 0, 0)
            })
            card.addView(texts)
            card.addView(TextView(this).apply {
                text = "›"
                setTextColor(textMuted)
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
            })
            return card
        }

        cards.add(modeCard("⚔", "生存模式", "消灭敌人 · 存活到底", wave1, 1))
        cards.add(modeCard("∞", "无尽狂潮", "无限敌人 · 挑战极限", wave2, 5))
        cards.add(modeCard("◎", "联机模式", "P2P 直连 · 合作对战", wave3, 4))
        cards.forEach { content.addView(it) }

        content.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        })

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }
        bottom.addView(TextView(this).apply {
            text = "  ⚙  设置  "
            setTextColor(textSecondary)
            textSize = 13f
            setPadding(dp(28), dp(10), dp(28), dp(10))
            background = GradientDrawable().apply {
                setColor(cardBg)
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), cardBorder)
            }
            setOnClickListener {
                val i = Intent(this@MainActivity, GameActivity::class.java)
                i.putExtra("mode", 0)
                i.putExtra("showSettings", true)
                startActivity(i)
            }
        })
        content.addView(bottom)
        content.addView(TextView(this).apply {
            text = "V3.2.0"
            setTextColor(textMuted)
            textSize = 10f
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, 0)
        })

        root.addView(content)
        setContentView(root)

        content.translationY = dp(40).toFloat()
        content.alpha = 0f
        content.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        cards.forEachIndexed { i, card ->
            card.translationY = dp(30).toFloat()
            card.alpha = 0f
            card.animate()
                .translationY(0f)
                .alpha(1f)
                .setStartDelay(150L + i * 100L)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator(1.5f))
                .start()
        }
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
