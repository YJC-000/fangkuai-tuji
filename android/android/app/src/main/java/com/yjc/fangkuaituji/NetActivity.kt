package com.yjc.fangkuaituji

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

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
    private lateinit var roomListContainer: LinearLayout
    private lateinit var roomListStatus: TextView
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val ROOM_API = "https://crudcrud.com/api/77ffb1c1c7424d8a9eeb5ed8e590fdab"
    }

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
            setPadding(0, dp(8), 0, dp(16))
        }
        val backBtn = makeButton("← 返回", "#3A3228", "#D4A574", 14f, dp(12)) {
            finish(); overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }.apply { layoutParams = LinearLayout.LayoutParams(dp(80), dp(40)) }
        val title = TextView(this).apply {
            text = "远程联机"
            textSize = 22f
            setTextColor(Color.parseColor("#F5EDE0"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f)
        }
        titleBar.addView(backBtn)
        titleBar.addView(title)
        titleBar.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(80), dp(40)) })
        root.addView(titleBar)

        root.addView(makeSectionTitle("在线房间"))
        val listCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg()
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val listHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        roomListStatus = TextView(this).apply {
            text = "正在刷新…"
            textSize = 12f
            setTextColor(Color.parseColor("#B0A498"))
            layoutParams = LinearLayout.LayoutParams(0, dp(30), 1f)
        }
        val refreshBtn = makeButton("🔄 刷新", "#3A3228", "#D4A574", 11f, dp(10)) {
            refreshRoomList()
        }.apply { layoutParams = LinearLayout.LayoutParams(dp(70), dp(32)) }
        val randomBtn = makeButton("🎲 随机进入", "#8BB876", "#1A1714", 11f, dp(10)) {
            randomJoin()
        }.apply { layoutParams = LinearLayout.LayoutParams(dp(90), dp(32)).apply { setMargins(dp(6), 0, 0, 0) } }
        listHeader.addView(roomListStatus)
        listHeader.addView(refreshBtn)
        listHeader.addView(randomBtn)
        listCard.addView(listHeader)

        roomListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        listCard.addView(roomListContainer)
        root.addView(listCard)

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
        nameEt = makeEdit("玩家${(100..999).random()}")
        roomEt = makeEdit("${(1000..9999).random()}")
        pwdEt = makeEdit("")
        card.addView(makeLabeledEdit("玩家昵称", nameEt))
        card.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(1), dp(10)) })
        card.addView(makeLabeledEdit("房间号", roomEt))
        card.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(1), dp(10)) })
        card.addView(makeLabeledEdit("房间密码（可选）", pwdEt))
        root.addView(card)

        val tip = TextView(this).apply {
            text = "远程联机基于 WebRTC P2P 直连。创建房间后会自动发布到房间列表，朋友可在列表中直接加入。"
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
            if (isCreate) {
                checkRoomAndCreate(room, name, pwd)
            } else {
                launchGame(name, room, pwd)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52))
        }
        root.addView(startBtn)

        scroll.addView(root)
        setContentView(scroll)

        refreshRoomList()
    }

    private fun checkRoomAndCreate(room: String, name: String, pwd: String) {
        roomListStatus.text = "检查房间号…"
        Thread {
            try {
                val rooms = fetchRooms()
                val taken = rooms.any { it.optString("roomId") == room }
                handler.post {
                    if (taken) {
                        roomListStatus.text = "❌ 房间号已被占用，请换一个"
                        Toast.makeText(this, "房间号已被占用", Toast.LENGTH_SHORT).show()
                    } else {
                        launchGame(name, room, pwd)
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    roomListStatus.text = "网络错误，直接创建"
                    launchGame(name, room, pwd)
                }
            }
        }.start()
    }

    private fun launchGame(name: String, room: String, pwd: String) {
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
    }

    private fun refreshRoomList() {
        roomListStatus.text = "正在刷新…"
        roomListContainer.removeAllViews()
        Thread {
            try {
                val rooms = fetchRooms()
                handler.post {
                    roomListContainer.removeAllViews()
                    if (rooms.length() == 0) {
                        roomListStatus.text = "暂无在线房间"
                        val empty = TextView(this).apply {
                            text = "暂无在线房间，创建一个吧！"
                            textSize = 12f
                            setTextColor(Color.parseColor("#6B6158"))
                            gravity = Gravity.CENTER
                            setPadding(0, dp(16), 0, dp(16))
                        }
                        roomListContainer.addView(empty)
                    } else {
                        roomListStatus.text = "共 ${rooms.length()} 个房间"
                        for (i in 0 until rooms.length()) {
                            val r = rooms.getJSONObject(i)
                            val roomId = r.optString("roomId", "?")
                            val hostName = r.optString("name", "玩家")
                            val hasPwd = r.optBoolean("hasPwd", false)
                            val players = r.optInt("players", 1)
                            roomListContainer.addView(makeRoomItem(roomId, hostName, hasPwd, players))
                        }
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    roomListStatus.text = "刷新失败：${e.message}"
                }
            }
        }.start()
    }

    private fun makeRoomItem(roomId: String, hostName: String, hasPwd: Boolean, players: Int): LinearLayout {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1714"))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.parseColor("#3A3228"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply {
                setMargins(0, dp(4), 0, dp(4))
            }
            isClickable = true
            setOnClickListener {
                roomEt.setText(roomId)
                isCreate = false
                refreshModeButtons()
                if (hasPwd) {
                    Toast.makeText(this@NetActivity, "房间有密码，请输入密码", Toast.LENGTH_SHORT).show()
                    pwdEt.requestFocus()
                } else {
                    pwdEt.setText("")
                }
            }
        }
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        info.addView(TextView(this).apply {
            text = "房间 $roomId"
            textSize = 14f
            setTextColor(Color.parseColor("#F5EDE0"))
        })
        info.addView(TextView(this).apply {
            text = "主机：$hostName · $players 人"
            textSize = 11f
            setTextColor(Color.parseColor("#B0A498"))
        })
        item.addView(info)
        if (hasPwd) {
            item.addView(TextView(this).apply {
                text = "🔒"
                textSize = 18f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
            })
        } else {
            item.addView(TextView(this).apply {
                text = "加入 →"
                textSize = 12f
                setTextColor(Color.parseColor("#8BB876"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(50), dp(36))
            })
        }
        return item
    }

    private fun randomJoin() {
        roomListStatus.text = "正在随机匹配…"
        Thread {
            try {
                val rooms = fetchRooms()
                val open = mutableListOf<org.json.JSONObject>()
                for (i in 0 until rooms.length()) {
                    val r = rooms.getJSONObject(i)
                    if (!r.optBoolean("hasPwd", false)) open.add(r)
                }
                handler.post {
                    if (open.isEmpty()) {
                        roomListStatus.text = "没有无密码的房间"
                        Toast.makeText(this, "没有可随机进入的房间", Toast.LENGTH_SHORT).show()
                    } else {
                        val r = open.random()
                        val roomId = r.optString("roomId")
                        roomEt.setText(roomId)
                        pwdEt.setText("")
                        isCreate = false
                        refreshModeButtons()
                        roomListStatus.text = "已选房间 $roomId，点击开始联机"
                    }
                }
            } catch (e: Exception) {
                handler.post { roomListStatus.text = "随机匹配失败" }
            }
        }.start()
    }

    private fun fetchRooms(): JSONArray {
        val url = URL("$ROOM_API/rooms")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) sb.append(line)
        reader.close()
        conn.disconnect()
        return JSONArray(sb.toString())
    }

    private fun makeLabeledEdit(label: String, et: EditText): LinearLayout {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val labelTv = TextView(this).apply {
            text = label
            textSize = 12f
            setTextColor(Color.parseColor("#B0A498"))
            setPadding(0, 0, 0, dp(4))
        }
        layout.addView(labelTv)
        layout.addView(et)
        return layout
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

    private fun makeEdit(defValue: String): EditText {
        return EditText(this).apply {
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
