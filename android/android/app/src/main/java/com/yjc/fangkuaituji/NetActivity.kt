package com.yjc.fangkuaituji

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URLEncoder
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
    private lateinit var typeSection: TextView
    private lateinit var typeRow: LinearLayout
    private val handler = Handler(Looper.getMainLooper())

    private val C_BG = "#1A1612"; private val C_CARD = "#2A241D"; private val C_STROKE = "#3D3428"
    private val C_INK = "#F5EDE0"; private val C_INK2 = "#B0A498"; private val C_MUTED = "#8A7F72"
    private val C_ORANGE = "#E8A04A"; private val C_GREEN = "#8BB876"; private val C_FIELD = "#221D18"

    companion object {
        private const val ROOM_API = "https://crudcrud.com/api/77ffb1c1c7424d8a9eeb5ed8e590fdab"
        private const val PUB_WEB_URL = "https://yjc-000.github.io/fangkuai-tuji/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        immersive()

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor(C_BG)); isVerticalScrollBarEnabled = false
            setPadding(dp(20), dp(16), dp(20), dp(40))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // 标题栏
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(12))
        }
        titleBar.addView(makeButton("← 返回", "#3A3228", C_ORANGE, 14f, 12) {
            finish(); overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }.apply { layoutParams = LinearLayout.LayoutParams(dp(84), dp(42)) })
        titleBar.addView(TextView(this).apply {
            text = "远程联机"; textSize = 22f; setTextColor(Color.parseColor(C_INK)); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f)
        })
        titleBar.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(84), dp(42)) })
        root.addView(titleBar)

        // 在线房间
        root.addView(section("在线房间"))
        val listCard = card()
        val listHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        }
        roomListStatus = TextView(this).apply {
            text = "正在刷新…"; textSize = 12f; setTextColor(Color.parseColor(C_INK2))
            layoutParams = LinearLayout.LayoutParams(0, dp(32), 1f)
        }
        listHeader.addView(roomListStatus)
        listHeader.addView(makeButton("🔄", "#3A3228", C_ORANGE, 12f, 10) { refreshRoomList() }
            .apply { layoutParams = LinearLayout.LayoutParams(dp(44), dp(34)).apply { setMargins(dp(6), 0, 0, 0) } })
        listHeader.addView(makeButton("🎲", C_GREEN, C_BG, 12f, 10) { randomJoin() }
            .apply { layoutParams = LinearLayout.LayoutParams(dp(44), dp(34)).apply { setMargins(dp(6), 0, 0, 0) } })
        listCard.addView(listHeader)
        roomListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, 0)
        }
        listCard.addView(roomListContainer)
        root.addView(listCard)

        // 房间操作：创建 / 加入
        root.addView(section("房间操作"))
        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), 0, dp(4))
        }
        createBtn = makeButton("创建房间", C_ORANGE, C_BG, 14f, 14) {
            isCreate = true; refreshModeButtons()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(6), 0) } }
        joinBtn = makeButton("加入房间", "#3A3228", C_ORANGE, 14f, 14) {
            isCreate = false; refreshModeButtons()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(6), 0, 0, 0) } }
        modeRow.addView(createBtn); modeRow.addView(joinBtn)
        root.addView(modeRow)

        // 游戏模式（仅创建时显示）
        typeSection = section("游戏模式")
        root.addView(typeSection)
        typeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), 0, dp(4))
        }
        coopBtn = makeButton("合作打怪", C_GREEN, C_BG, 14f, 14) {
            isCoop = true; refreshTypeButtons()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(6), 0) } }
        vsBtn = makeButton("玩家对战", "#3A3228", C_ORANGE, 14f, 14) {
            isCoop = false; refreshTypeButtons()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(6), 0, 0, 0) } }
        typeRow.addView(coopBtn); typeRow.addView(vsBtn)
        root.addView(typeRow)

        // 房间信息
        root.addView(section("房间信息"))
        val infoCard = card()
        nameEt = makeEdit("玩家昵称（留空随机）")
        roomEt = makeEdit("房间号（4 位）")
        pwdEt = makeEdit("房间密码（可选）")
        roomEt.setText("${(1000..9999).random()}")
        nameEt.setText("玩家${(100..999).random()}")
        infoCard.addView(labeled("玩家昵称", nameEt))
        infoCard.addView(spacer(10)); infoCard.addView(labeled("房间号", roomEt))
        infoCard.addView(spacer(10)); infoCard.addView(labeled("房间密码（可选）", pwdEt))
        root.addView(infoCard)

        // 快捷网址
        val linkBtn = makeButton("🔗 复制快捷网址（手机浏览器直连进房）", "#3A3228", C_ORANGE, 12f, 14) {
            copyQuickLink()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
                .apply { setMargins(0, dp(12), 0, 0) }
        }
        root.addView(linkBtn)

        root.addView(TextView(this).apply {
            text = "远程联机：房间发布在托管目录，数据走 WebRTC P2P 直连。创建房间后朋友可在列表直接加入；把快捷网址发给没装 App 的人，用浏览器打开即可进房。"
            textSize = 10f; setTextColor(Color.parseColor(C_MUTED))
            setPadding(dp(4), dp(10), dp(4), dp(16))
        })

        root.addView(makeButton("开始联机", C_ORANGE, C_BG, 17f, 18) {
            val name = nameEt.text.toString().trim().ifEmpty { "玩家${(100..999).random()}" }
            val room = roomEt.text.toString().trim()
            val pwd = pwdEt.text.toString().trim()
            if (room.isEmpty()) { toast("请输入房间号"); return@makeButton }
            if (isCreate) checkRoomAndCreate(room, name, pwd) else launchGame(name, room, pwd, isCoop)
        }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)) })

        scroll.addView(root)
        setContentView(scroll)
        refreshModeButtons(); refreshTypeButtons()
        refreshRoomList()
    }

    private fun copyQuickLink() {
        val room = roomEt.text.toString().trim()
        if (room.isEmpty()) { toast("请先填写房间号"); return }
        val pwd = pwdEt.text.toString().trim()
        val mode = if (isCoop) 4 else 6
        var url = "${PUB_WEB_URL}?mode=$mode&autostart=1&p2p=1&create=0&room=${enc(room)}"
        if (pwd.isNotEmpty()) url += "&pwd=${enc(pwd)}"
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("fangkuai-link", url))
        toast("快捷网址已复制，发给朋友即可进房")
    }

    private fun checkRoomAndCreate(room: String, name: String, pwd: String) {
        roomListStatus.text = "检查房间号…"
        Thread {
            try {
                val rooms = fetchRooms()
                val taken = (0 until rooms.length()).any { rooms.getJSONObject(it).optString("roomId") == room }
                handler.post {
                    if (taken) {
                        roomListStatus.text = "❌ 房间号已被占用，请换一个"
                        toast("房间号已被占用")
                    } else launchGame(name, room, pwd, isCoop)
                }
            } catch (e: Exception) {
                handler.post { roomListStatus.text = "网络异常，仍尝试创建"; launchGame(name, room, pwd, isCoop) }
            }
        }.start()
    }

    private fun launchGame(name: String, room: String, pwd: String, coop: Boolean) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("isNet", true)
            putExtra("create", isCreate)
            putExtra("coop", coop)
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
                        roomListContainer.addView(TextView(this).apply {
                            text = "暂无在线房间，创建一个吧！"; textSize = 12f
                            setTextColor(Color.parseColor(C_MUTED)); gravity = Gravity.CENTER
                            setPadding(0, dp(18), 0, dp(18))
                        })
                    } else {
                        roomListStatus.text = "共 ${rooms.length()} 个房间"
                        for (i in 0 until rooms.length()) {
                            val r = rooms.getJSONObject(i)
                            roomListContainer.addView(makeRoomItem(
                                r.optString("roomId", "?"),
                                r.optString("name", "玩家"),
                                r.optBoolean("hasPwd", false),
                                r.optInt("players", 1),
                                r.optInt("mode", 4)
                            ))
                        }
                    }
                }
            } catch (e: Exception) {
                handler.post { roomListStatus.text = "刷新失败：${e.message ?: "网络错误"}（点 🔄 重试）" }
            }
        }.start()
    }

    private fun makeRoomItem(roomId: String, hostName: String, hasPwd: Boolean, players: Int, mode: Int): LinearLayout {
        val modeLabel = if (mode == 6) "对战" else "合作"
        val modeColor = if (mode == 6) C_ORANGE else C_GREEN
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(C_FIELD, 14).apply { setStroke(dp(1), Color.parseColor(C_STROKE)) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(4), 0, dp(4)) }
            isClickable = true
            setOnClickListener {
                roomEt.setText(roomId)
                isCreate = false; refreshModeButtons()
                isCoop = (mode != 6); refreshTypeButtons()
                if (hasPwd) {
                    pwdEt.setText(""); pwdEt.requestFocus()
                    toast("房间有密码，请输入密码后点开始联机")
                } else {
                    pwdEt.setText("")
                    toast("正在进入房间 $roomId")
                    launchGame(nameEt.text.toString().trim().ifEmpty { "玩家${(100..999).random()}" },
                        roomId, "", isCoop)
                }
            }
        }
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        info.addView(TextView(this).apply {
            text = "房间 $roomId"; textSize = 14f; setTextColor(Color.parseColor(C_INK))
        })
        info.addView(TextView(this).apply {
            text = "$hostName · $players 人 · $modeLabel"; textSize = 11f
            setTextColor(Color.parseColor(C_INK2))
        })
        item.addView(info)
        item.addView(TextView(this).apply {
            text = if (hasPwd) "🔒" else "$modeLabel →"; textSize = if (hasPwd) 18f else 12f
            setTextColor(Color.parseColor(if (hasPwd) C_ORANGE else modeColor))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(40))
        })
        return item
    }

    private fun randomJoin() {
        roomListStatus.text = "正在随机匹配…"
        Thread {
            try {
                val rooms = fetchRooms()
                val open = mutableListOf<JSONObject>()
                for (i in 0 until rooms.length()) {
                    val r = rooms.getJSONObject(i)
                    if (!r.optBoolean("hasPwd", false)) open.add(r)
                }
                handler.post {
                    if (open.isEmpty()) {
                        roomListStatus.text = "没有无密码房间"; toast("没有可随机进入的房间")
                    } else {
                        val r = open.random()
                        val roomId = r.optString("roomId")
                        val coop = r.optInt("mode", 4) != 6
                        roomListStatus.text = "已匹配房间 $roomId"
                        launchGame(nameEt.text.toString().trim().ifEmpty { "玩家${(100..999).random()}" },
                            roomId, "", coop)
                    }
                }
            } catch (e: Exception) {
                handler.post { roomListStatus.text = "随机匹配失败，请重试" }
            }
        }.start()
    }

    // 带 3 次重试的房间列表拉取
    private fun fetchRooms(): JSONArray {
        var lastErr: Exception? = null
        repeat(3) { attempt ->
            try {
                val conn = URL("$ROOM_API/rooms").openConnection() as HttpURLConnection
                conn.requestMethod = "GET"; conn.connectTimeout = 8000; conn.readTimeout = 8000
                conn.setRequestProperty("Accept", "application/json")
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                val sb = StringBuilder(); var line: String?
                while (reader.readLine().also { line = it } != null) sb.append(line)
                reader.close(); conn.disconnect()
                if (code in 200..299) return JSONArray(sb.toString())
                lastErr = Exception("HTTP $code")
            } catch (e: Exception) {
                lastErr = e
            }
            try { Thread.sleep(700L * (attempt + 1)) } catch (_: Exception) {}
        }
        throw lastErr ?: Exception("获取房间列表失败")
    }

    private fun refreshModeButtons() {
        setBtnStyle(createBtn, if (isCreate) C_ORANGE else "#3A3228", if (isCreate) C_BG else C_ORANGE)
        setBtnStyle(joinBtn, if (!isCreate) C_ORANGE else "#3A3228", if (!isCreate) C_BG else C_ORANGE)
        // 加入房间时不需要选模式（跟随主机），隐藏模式行
        val vis = if (isCreate) View.VISIBLE else View.GONE
        typeSection.visibility = vis; typeRow.visibility = vis
    }

    private fun refreshTypeButtons() {
        setBtnStyle(coopBtn, if (isCoop) C_GREEN else "#3A3228", if (isCoop) C_BG else C_ORANGE)
        setBtnStyle(vsBtn, if (!isCoop) C_ORANGE else "#3A3228", if (!isCoop) C_BG else C_ORANGE)
    }

    private fun setBtnStyle(btn: Button, bg: String, tc: String) {
        btn.background = rounded(bg, 14); btn.setTextColor(Color.parseColor(tc))
    }

    private fun section(t: String) = TextView(this).apply {
        text = t; textSize = 15f; setTextColor(Color.parseColor(C_ORANGE)); setPadding(0, dp(16), 0, dp(8))
    }

    private fun spacer(h: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(h))
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; background = cardBg()
        setPadding(dp(16), dp(16), dp(16), dp(16))
    }

    private fun labeled(label: String, et: EditText): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(this@NetActivity).apply {
            text = label; textSize = 12f; setTextColor(Color.parseColor(C_INK2))
            setPadding(0, 0, 0, dp(4))
        })
        addView(et)
    }

    private fun makeEdit(hint: String): EditText = EditText(this).apply {
        this.hint = hint; setTextColor(Color.parseColor(C_INK))
        setHintTextColor(Color.parseColor(C_MUTED)); textSize = 14f
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = rounded(C_FIELD, 12).apply { setStroke(dp(1), Color.parseColor(C_STROKE)) }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
    }

    private fun makeButton(text: String, bgColor: String, textColor: String, textSize: Float, radiusDp: Int,
                           onClick: (() -> Unit)? = null): Button = Button(this).apply {
        this.text = text; this.textSize = textSize
        setTextColor(Color.parseColor(textColor)); background = rounded(bgColor, radiusDp)
        setPadding(dp(8), 0, dp(8), 0); isAllCaps = false; stateListAnimator = null
        if (onClick != null) setOnClickListener { onClick() }
    }

    private fun cardBg(): GradientDrawable = rounded(C_CARD, 20).apply { setStroke(dp(1), Color.parseColor(C_STROKE)) }
    private fun rounded(color: String, radiusDp: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(color)); cornerRadius = dp(radiusDp).toFloat()
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()
    private fun immersive() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus); if (hasFocus) immersive()
    }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
