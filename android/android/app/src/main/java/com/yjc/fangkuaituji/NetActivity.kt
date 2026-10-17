package com.yjc.fangkuaituji

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class NetActivity : AppCompatActivity() {

    private var isCreate = true
    private var isCoop = true
    private lateinit var createBtn: Button
    private lateinit var joinBtn: Button
    private lateinit var coopBtn: Button
    private lateinit var vsBtn: Button
    private lateinit var nameEt: EditText
    private lateinit var roomEt: EditText
    private lateinit var pwdEt: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

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
            text = "联机对战"
            textSize = 22f
            setTextColor(Color.parseColor("#F5EDE0"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f)
        }
        titleBar.addView(backBtn)
        titleBar.addView(title)
        titleBar.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(80), dp(40)) })
        root.addView(titleBar)

        root.addView(makeSectionTitle("房间操作"))
        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(4))
        }
        createBtn = makeButton("创建房间", "#E8A04A", "#1A1714", 14f, dp(14)) {
            isCreate = true; refreshModeButtons()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(0, 0, dp(6), 0) } }
        joinBtn = makeButton("加入房间", "#3A3228", "#D4A574", 14f, dp(14)) {
            isCreate = false; refreshModeButtons()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(6), 0, 0, 0) } }
        modeRow.addView(createBtn)
        modeRow.addView(joinBtn)
        root.addView(modeRow)

        root.addView(makeSectionTitle("游戏模式"))
        val typeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(4))
        }
        coopBtn = makeButton("合作打怪", "#8BB876", "#1A1714", 14f, dp(14)) {
            isCoop = true; refreshTypeButtons()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(0, 0, dp(6), 0) } }
        vsBtn = makeButton("玩家对战", "#3A3228", "#D4A574", 14f, dp(14)) {
            isCoop = false; refreshTypeButtons()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(6), 0, 0, 0) } }
        typeRow.addView(coopBtn)
        typeRow.addView(vsBtn)
        root.addView(typeRow)

        root.addView(makeSectionTitle("房间信息"))
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg()
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        nameEt = makeEdit("玩家昵称", "玩家${(100..999).random()}")
        roomEt = makeEdit("房间号", "${(1000..9999).random()}")
        pwdEt = makeEdit("房间密码（可选）", "")
        card.addView(nameEt)
        card.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(1), dp(10)) })
        card.addView(roomEt)
        card.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(1), dp(10)) })
        card.addView(pwdEt)
        root.addView(card)

        val tip = TextView(this).apply {
            text = "手机端使用 WebRTC P2P 直连，无需外部服务器。创建房间后把房间号和密码告诉朋友即可加入。"
            textSize = 11f
            setTextColor(Color.parseColor("#6B6158"))
            setPadding(dp(4), dp(12), dp(4), dp(20))
        }
        root.addView(tip)

        val startBtn = makeButton("开始联机", "#E8A04A", "#1A1714", 16f, dp(16)) {
            val name = nameEt.text.toString().trim().ifEmpty { "玩家${(100..999).random()}" }
            val room = roomEt.text.toString().trim()
            val pwd = pwdEt.text.toString().trim()
            if (room.isEmpty()) {
                Toast.makeText(this, "请输入房间号", Toast.LENGTH_SHORT).show()
                return@makeButton
            }
            val intent = Intent(this, GameActivity::class.java).apply {
                putExtra("isNet", true)
                putExtra("create", isCreate)
                putExtra("coop", isCoop)
                putExtra("name", name)
                putExtra("room", room)
                putExtra("pwd", pwd)
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52))
        }
        root.addView(startBtn)

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun refreshModeButtons() {
        setBtnStyle(createBtn, if (isCreate) "#E8A04A" else "#3A3228", if (isCreate) "#1A1714" else "#D4A574")
        setBtnStyle(joinBtn, if (!isCreate) "#E8A04A" else "#3A3228", if (!isCreate) "#1A1714" else "#D4A574")
    }

    private fun refreshTypeButtons() {
        setBtnStyle(coopBtn, if (isCoop) "#8BB876" else "#3A3228", if (isCoop) "#1A1714" else "#D4A574")
        setBtnStyle(vsBtn, if (!isCoop) "#E8A04A" else "#3A3228", if (!isCoop) "#1A1714" else "#D4A574")
    }

    private fun setBtnStyle(btn: Button, bg: String, tc: String) {
        btn.background = GradientDrawable().apply {
            setColor(Color.parseColor(bg))
            cornerRadius = dp(14).toFloat()
        }
        btn.setTextColor(Color.parseColor(tc))
    }

    private fun makeSectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.parseColor("#E8A04A"))
            setPadding(0, dp(16), 0, dp(8))
        }
    }

    private fun makeEdit(label: String, defValue: String): LinearLayout {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val labelTv = TextView(this).apply {
            this.text = label
            textSize = 12f
            setTextColor(Color.parseColor("#B0A498"))
            setPadding(0, 0, 0, dp(4))
        }
        val et = EditText(this).apply {
            setText(defValue)
            setTextColor(Color.parseColor("#F5EDE0"))
            setHintTextColor(Color.parseColor("#6B6158"))
            textSize = 14f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1714"))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), Color.parseColor("#3A3228"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
        }
        layout.addView(labelTv)
        layout.addView(et)
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
