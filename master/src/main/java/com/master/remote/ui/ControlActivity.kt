package com.master.remote.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.master.remote.R
import com.master.remote.net.WsClient

class ControlActivity : AppCompatActivity() {

    private var ws: WsClient? = null
    private var screenView: ImageView? = null
    private var tvStatus: TextView? = null
    private var tvFps: TextView? = null
    private val gson = Gson()

    private var screenW = 1080
    private var screenH = 1920
    private var viewW = 0
    private var viewH = 0
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var frameCount = 0
    private var lastFpsTime = 0L
    private var quality = 70
    private var fps = 15
    private var keepScreenOn = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_control)
        } catch (e: Throwable) {
            finish()
            return
        }

        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Throwable) {}

        var ip: String? = intent.getStringExtra("ip")
        if (ip == null) ip = "127.0.0.1"
        val port: Int = intent.getIntExtra("port", 8888)
        var name: String? = intent.getStringExtra("name")
        if (name == null) name = "Device"

        try {
            screenView = findViewById(R.id.screenView) as ImageView?
            tvStatus = findViewById(R.id.tvStatus) as TextView?
            tvFps = findViewById(R.id.tvFps) as TextView?

            val tvTitle = findViewById(R.id.tvTitle) as TextView?
            val tvInfo = findViewById(R.id.tvInfo) as TextView?

            if (tvTitle != null) tvTitle.text = name
            if (tvInfo != null) tvInfo.text = ip + ":" + port
            if (tvStatus != null) tvStatus!!.text = "连接中..."
        } catch (e: Throwable) {}

        connectWs(ip, port)
        setupTouch()
        setupButtons()
    }

    private fun connectWs(ip: String, port: Int) {
        val onOpenCb: () -> Unit = object : Function0<Unit> {
            override fun invoke() {
                runOnUiThread(object : Runnable {
                    override fun run() {
                        if (tvStatus != null) tvStatus!!.text = "已连接"
                    }
                })
            }
        }
        // Kotlin lambda for WsClient
        ws = WsClient(
            { runOnUiThread { run2 { if (tvStatus != null) tvStatus!!.text = "已连接" } } },
            { msg -> handleMessage(msg) },
            { r -> runOnUiThread { if (tvStatus != null) tvStatus!!.text = "断开: " + r } }
        )
        try {
            ws!!.connect(ip, port)
        } catch (e: Throwable) {}

        val h = Handler(Looper.getMainLooper())
        h.postDelayed(object : Runnable {
            override fun run() {
                try {
                    ws!!.send("{\"type\":\"info\"}")
                    ws!!.send("{\"type\":\"screen_start\"}")
                } catch (e: Throwable) {}
            }
        }, 800L)
    }

    private fun run2(f: () -> Unit) {
        try { f() } catch (e: Throwable) {}
    }

    private fun setupTouch() {
        try {
            val sv = screenView
            if (sv != null) {
                sv.setOnTouchListener(object : View.OnTouchListener {
                    override fun onTouch(v: View, ev: MotionEvent): Boolean {
                        try {
                            if (ev.action == MotionEvent.ACTION_DOWN) {
                                downX = ev.x
                                downY = ev.y
                                downTime = System.currentTimeMillis()
                            } else if (ev.action == MotionEvent.ACTION_UP) {
                                val dx = Math.abs(ev.x - downX)
                                val dy = Math.abs(ev.y - downY)
                                val dt = System.currentTimeMillis() - downTime
                                if (dx < 30f && dy < 30f) {
                                    if (dt > 600L) {
                                        sendCmd("longpress", toX(ev.x), toY(ev.y), 800L)
                                    } else {
                                        sendCmd("tap", toX(ev.x), toY(ev.y), 0L)
                                    }
                                } else {
                                    sendSwipe(downX, downY, ev.x, ev.y, dt)
                                }
                            }
                        } catch (e: Throwable) {}
                        return true
                    }
                })
            }
        } catch (e: Throwable) {}
    }

    private fun setupButtons() {
        setupSimple(R.id.btnBack, "back")
        setupSimple(R.id.btnHome, "home")
        setupSimple(R.id.btnRecents, "recents")
        setupSimple(R.id.btnMute, "mute")
        setupSimple(R.id.btnLock, "lock")
        setupSimple(R.id.btnScreenshot, "screenshot")
        setupSimple(R.id.btnRotate, "rotate")
        setupSimple(R.id.btnApps, "app_list")
        setupSimple(R.id.btnDeviceInfo, "device_info")
        setupSimple(R.id.btnScreenToggle, "screen_toggle")
        setupSimple(R.id.btnRestart, "restart")
        setupSimple(R.id.btnVibrate, "vibrate")
        setupSimple(R.id.btnSpeak, "speak_demo")
        setupSimple(R.id.btnKeyPower, "key_power")
        setupSimple(R.id.btnKeyEnter, "key_enter")
        setupSimple(R.id.btnKeyEsc, "key_esc")

        setupInt(R.id.btnVolUp, "volume", 1)
        setupInt(R.id.btnVolDown, "volume", -1)
        setupInt(R.id.btnBrUp, "brightness", 1)
        setupInt(R.id.btnBrDown, "brightness", -1)

        setupSimple(R.id.btnClose, "close")
        setupPaste()
        setupText()
        setupQuality()
        setupFps()
        setupKeepOn()
    }

    private fun setupSimple(id: Int, cmd: String) {
        try {
            val v = findViewById(id) as View?
            if (v != null) {
                v.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(view: View) {
                        if (cmd == "close") {
                            finish()
                        } else {
                            sendCmd(cmd, 0f, 0f, 0L)
                        }
                    }
                })
            }
        } catch (e: Throwable) {}
    }

    private fun setupInt(id: Int, cmd: String, value: Int) {
        try {
            val v = findViewById(id) as View?
            if (v != null) {
                v.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(view: View) {
                        sendCmdInt(cmd, value)
                    }
                })
            }
        } catch (e: Throwable) {}
    }

    private fun setupPaste() {
        try {
            val v = findViewById(R.id.btnPaste) as View?
            if (v != null) {
                v.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(view: View) {
                        try {
                            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = cm.primaryClip
                            var text = ""
                            if (clip != null) {
                                val item = clip.getItemAt(0)
                                if (item != null) {
                                    val cs = item.text
                                    if (cs != null) text = cs.toString()
                                }
                            }
                            sendCmdText("clipboard", text)
                            toast("已发送剪贴板")
                        } catch (e: Throwable) {}
                    }
                })
            }
        } catch (e: Throwable) {}
    }

    private fun setupText() {
        try {
            val v = findViewById(R.id.btnText) as View?
            if (v != null) {
                v.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(view: View) {
                        showInputDialog()
                    }
                })
            }
        } catch (e: Throwable) {}
    }

    private fun setupQuality() {
        try {
            val v = findViewById(R.id.btnQuality) as View?
            if (v != null) {
                v.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(view: View) {
                        if (quality == 70) quality = 90
                        else if (quality == 90) quality = 50
                        else quality = 70
                        sendCmdInt("setQuality", quality)
                        toast("画质: " + quality)
                    }
                })
            }
        } catch (e: Throwable) {}
    }

    private fun setupFps() {
        try {
            val v = findViewById(R.id.btnFps) as View?
            if (v != null) {
                v.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(view: View) {
                        if (fps == 15) fps = 30
                        else if (fps == 30) fps = 10
                        else if (fps == 10) fps = 5
                        else fps = 15
                        sendCmdInt("setFps", fps)
                        toast("帧率: " + fps + " fps")
                    }
                })
            }
        } catch (e: Throwable) {}
    }

    private fun setupKeepOn() {
        try {
            val v = findViewById(R.id.btnKeepOn) as View?
            if (v != null) {
                v.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(view: View) {
                        keepScreenOn = !keepScreenOn
                        try {
                            if (keepScreenOn) {
                                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            } else {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            }
                        } catch (e: Throwable) {}
                        if (keepScreenOn) toast("常亮: 开") else toast("常亮: 关")
                    }
                })
            }
        } catch (e: Throwable) {}
    }

    private fun showInputDialog() {
        try {
            val ed = EditText(this)
            ed.hint = "输入要发送的文字"
            val builder = AlertDialog.Builder(this)
            builder.setTitle("发送文字到被控端")
            builder.setView(ed)
            builder.setPositiveButton("发送", object : DialogInterface.OnClickListener {
                override fun onClick(d: DialogInterface, which: Int) {
                    sendCmdText("text", ed.text.toString())
                }
            })
            builder.setNegativeButton("取消", null)
            builder.show()
        } catch (e: Throwable) {}
    }

    private fun toast(s: String) {
        try {
            Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {}
    }

    private fun toX(vx: Float): Float {
        if (viewW == 0) return vx
        return vx / viewW * screenW
    }

    private fun toY(vy: Float): Float {
        if (viewH == 0) return vy
        return vy / viewH * screenH
    }

    private fun sendCmd(type: String, x: Float, y: Float, dur: Long) {
        try {
            val obj = JsonObject()
            obj.addProperty("type", type)
            obj.addProperty("x", x)
            obj.addProperty("y", y)
            obj.addProperty("duration", dur)
            val w = ws
            if (w != null) w.send(obj.toString())
        } catch (e: Throwable) {}
    }

    private fun sendCmdInt(type: String, v: Int) {
        try {
            val obj = JsonObject()
            obj.addProperty("type", type)
            obj.addProperty("extra", v)
            val w = ws
            if (w != null) w.send(obj.toString())
        } catch (e: Throwable) {}
    }

    private fun sendCmdText(type: String, text: String) {
        try {
            val obj = JsonObject()
            obj.addProperty("type", type)
            obj.addProperty("text", text)
            val w = ws
            if (w != null) w.send(obj.toString())
        } catch (e: Throwable) {}
    }

    private fun sendSwipe(x1: Float, y1: Float, x2: Float, y2: Float, dt: Long) {
        try {
            val obj = JsonObject()
            obj.addProperty("type", "swipe")
            obj.addProperty("x1", toX(x1))
            obj.addProperty("y1", toY(y1))
            obj.addProperty("x2", toX(x2))
            obj.addProperty("y2", toY(y2))
            obj.addProperty("duration", if (dt > 100) dt else 100L)
            val w = ws
            if (w != null) w.send(obj.toString())
        } catch (e: Throwable) {}
    }

    private fun handleMessage(msg: String) {
        try {
            val j = gson.fromJson(msg, JsonObject::class.java)
            val type = j.get("type")
            var typeStr = ""
            if (type != null) typeStr = type.asString

            if (typeStr == "info") {
                val w = j.get("w")
                val h = j.get("h")
                if (w != null) screenW = w.asInt
                if (h != null) screenH = h.asInt
            } else if (typeStr == "frame") {
                val data = j.get("data")
                if (data == null) return
                val base64 = data.asString
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp == null) return
                runOnUiThread(object : Runnable {
                    override fun run() {
                        try {
                            val sv = screenView
                            if (sv != null) {
                                viewW = sv.width
                                viewH = sv.height
                                sv.setImageBitmap(bmp)
                            }
                            frameCount++
                            val now = System.currentTimeMillis()
                            if (now - lastFpsTime >= 1000L) {
                                val t = tvFps
                                if (t != null) t.text = "FPS: " + frameCount
                                frameCount = 0
                                lastFpsTime = now
                            }
                        } catch (e: Throwable) {}
                    }
                })
            } else if (typeStr == "clipboard") {
                val data = j.get("text")
                if (data == null) return
                val text = data.asString
                runOnUiThread(object : Runnable {
                    override fun run() {
                        try {
                            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("remote", text))
                            toast("收到剪贴板")
                        } catch (e: Throwable) {}
                    }
                })
            } else if (typeStr == "device_info") {
                val data = j.get("data")
                if (data != null) {
                    val info = data.asString
                    runOnUiThread(object : Runnable {
                        override fun run() {
                            showDialog("设备信息", info)
                        }
                    })
                }
            } else if (typeStr == "app_list") {
                val data = j.get("data")
                if (data != null) {
                    val list = data.asString
                    runOnUiThread(object : Runnable {
                        override fun run() {
                            showDialog("已安装应用", list)
                        }
                    })
                }
            }
        } catch (e: Throwable) {}
    }

    private fun showDialog(title: String, msg: String) {
        try {
            val b = AlertDialog.Builder(this)
            b.setTitle(title)
            b.setMessage(msg)
            b.setPositiveButton("确定", null)
            b.show()
        } catch (e: Throwable) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            val w = ws
            if (w != null) {
                w.send("{\"type\":\"screen_stop\"}")
                w.close()
            }
        } catch (e: Throwable) {}
    }
}
