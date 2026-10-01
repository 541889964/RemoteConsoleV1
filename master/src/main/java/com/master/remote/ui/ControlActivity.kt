package com.master.remote.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
    private var lastFpsTime = System.currentTimeMillis()
    private var quality = 70
    private var fps = 15
    private var keepScreenOn = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_control) } catch (_: Throwable) { finish(); return }

        // 保持屏幕常亮
        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (_: Throwable) {}

        val ip = intent.getStringExtra("ip") ?: "127.0.0.1"
        val port = intent.getIntExtra("port", 8888)
        val name = intent.getStringExtra("name") ?: "Device"

        try {
            screenView = findViewById(R.id.screenView) as? ImageView
            tvStatus = findViewById(R.id.tvStatus) as? TextView
            tvFps = findViewById(R.id.tvFps) as? TextView
            (findViewById(R.id.tvTitle) as? TextView)?.text = name
            (findViewById(R.id.tvInfo) as? TextView)?.text = ip + ":" + port
            if (tvStatus != null) tvStatus!!.text = "连接中..."
        } catch (_: Throwable) {}

        connectWs(ip, port)
        setupTouch()
        setupButtons()
    }

    private fun connectWs(ip: String, port: Int) {
        ws = WsClient(
            { runOnUiThread { if (tvStatus != null) tvStatus!!.text = "已连接" } },
            { msg -> handleMessage(msg) },
            { r -> runOnUiThread { if (tvStatus != null) tvStatus!!.text = "断开: " + r } }
        )
        try { ws?.connect(ip, port) } catch (_: Throwable) {}
        // 延迟请求 info 和 开启屏幕流
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                ws?.send("{\"type\":\"info\"}")
                ws?.send("{\"type\":\"screen_start\"}")
            } catch (_: Throwable) {}
        }, 800L)
    }

    private fun setupTouch() {
        try {
            screenView?.setOnTouchListener { _, ev ->
                try {
                    when (ev.action) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = ev.x; downY = ev.y
                            downTime = System.currentTimeMillis()
                        }
                        MotionEvent.ACTION_UP -> {
                            val dx = Math.abs(ev.x - downX)
                            val dy = Math.abs(ev.y - downY)
                            val dt = System.currentTimeMillis() - downTime
                            if (dx < 30 && dy < 30) {
                                if (dt > 600) sendCmd("longpress", toX(ev.x), toY(ev.y), 800)
                                else sendCmd("tap", toX(ev.x), toY(ev.y), 0)
                            } else sendSwipe(downX, downY, ev.x, ev.y, dt)
                        }
                    }
                } catch (_: Throwable) {}
                true
            }
        } catch (_: Throwable) {}
    }

    private fun setupButtons() {
        bindSimpleButton(R.id.btnBack, "back")
        bindSimpleButton(R.id.btnHome, "home")
        bindSimpleButton(R.id.btnRecents, "recents")
        bindSimpleButton(R.id.btnMute, "mute")
        bindSimpleButton(R.id.btnLock, "lock")
        bindSimpleButton(R.id.btnScreenshot, "screenshot")
        bindSimpleButton(R.id.btnRotate, "rotate")
        bindSimpleButton(R.id.btnApps, "app_list")
        bindSimpleButton(R.id.btnDeviceInfo, "device_info")
        bindSimpleButton(R.id.btnScreenToggle, "screen_toggle")
        bindSimpleButton(R.id.btnRestart, "restart")
        bindSimpleButton(R.id.btnVibrate, "vibrate")
        bindSimpleButton(R.id.btnSpeak, "speak_demo")
        bindSimpleButton(R.id.btnKeyPower, "key_power")
        bindSimpleButton(R.id.btnKeyEnter, "key_enter")
        bindSimpleButton(R.id.btnKeyEsc, "key_esc")

        bindIntButton(R.id.btnVolUp, "volume", 1)
        bindIntButton(R.id.btnVolDown, "volume", -1)
        bindIntButton(R.id.btnBrUp, "brightness", 1)
        bindIntButton(R.id.btnBrDown, "brightness", -1)

        bindSimpleButton(R.id.btnClose, "close")

        bindTapOnly(R.id.btnPaste, {
            try {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val text = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                sendCmdText("clipboard", text)
                toast("已发送剪贴板: " + text.take(20))
            } catch (_: Throwable) {}
        })

        bindTapOnly(R.id.btnText, { showInputDialog() })

        bindTapOnly(R.id.btnQuality, {
            if (quality == 70) quality = 90
            else if (quality == 90) quality = 50
            else quality = 70
            sendCmdInt("setQuality", quality)
            toast("画质: " + quality)
        })

        bindTapOnly(R.id.btnFps, {
            if (fps == 15) fps = 30
            else if (fps == 30) fps = 10
            else if (fps == 10) fps = 5
            else fps = 15
            sendCmdInt("setFps", fps)
            toast("帧率: " + fps + "fps")
        })

        bindTapOnly(R.id.btnKeepOn, {
            keepScreenOn = !keepScreenOn
            try {
                if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } catch (_: Throwable) {}
            toast("常亮: " + if (keepScreenOn) "开" else "关")
        })
    }

    private fun bindSimpleButton(id: Int, cmd: String) {
        try {
            (findViewById(id) as? View)?.setOnClickListener {
                if (cmd == "close") { finish(); return@setOnClickListener }
                sendCmd(cmd, 0f, 0f, 0)
            }
        } catch (_: Throwable) {}
    }

    private fun bindIntButton(id: Int, cmd: String, v: Int) {
        try {
            (findViewById(id) as? View)?.setOnClickListener {
                sendCmdInt(cmd, v)
            }
        } catch (_: Throwable) {}
    }

    private fun bindTapOnly(id: Int, action: () -> Unit) {
        try {
            (findViewById(id) as? View)?.setOnClickListener { action() }
        } catch (_: Throwable) {}
    }

    private fun showInputDialog() {
        try {
            val ed = EditText(this)
            ed.hint = "输入要发送的文字"
            AlertDialog.Builder(this)
                .setTitle("发送文字到被控端")
                .setView(ed)
                .setPositiveButton("发送") { _, _ -> sendCmdText("text", ed.text.toString()) }
                .setNegativeButton("取消", null)
                .show()
        } catch (_: Throwable) {}
    }

    private fun toast(s: String) {
        try { Toast.makeText(this, s, Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
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
            ws?.send(obj.toString())
        } catch (_: Throwable) {}
    }

    private fun sendCmdInt(type: String, v: Int) {
        try {
            val obj = JsonObject()
            obj.addProperty("type", type)
            obj.addProperty("extra", v)
            ws?.send(obj.toString())
        } catch (_: Throwable) {}
    }

    private fun sendCmdText(type: String, text: String) {
        try {
            val obj = JsonObject()
            obj.addProperty("type", type)
            obj.addProperty("text", text)
            ws?.send(obj.toString())
        } catch (_: Throwable) {}
    }

    private fun sendSwipe(x1: Float, y1: Float, x2: Float, y2: Float, dt: Long) {
        try {
            val obj = JsonObject()
            obj.addProperty("type", "swipe")
            obj.addProperty("x1", toX(x1))
            obj.addProperty("y1", toY(y1))
            obj.addProperty("x2", toX(x2))
            obj.addProperty("y2", toY(y2))
            obj.addProperty("duration", if (dt > 100) dt else 100)
            ws?.send(obj.toString())
        } catch (_: Throwable) {}
    }

    private fun handleMessage(msg: String) {
        try {
            val j = gson.fromJson(msg, JsonObject::class.java)
            val type = j.get("type")?.asString ?: return
            when (type) {
                "info" -> {
                    val w = j.get("w")?.asInt
                    val h = j.get("h")?.asInt
                    if (w != null) screenW = w
                    if (h != null) screenH = h
                }
                "frame" -> {
                    val data = j.get("data")?.asString ?: return
                    val bytes = Base64.decode(data, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
                    runOnUiThread {
                        try {
                            if (screenView != null) {
                                viewW = screenView!!.width
                                viewH = screenView!!.height
                                screenView!!.setImageBitmap(bmp)
                            }
                            // 更新 FPS
                            frameCount++
                            val now = System.currentTimeMillis()
                            if (now - lastFpsTime >= 1000) {
                                if (tvFps != null) tvFps!!.text = "FPS: " + frameCount
                                frameCount = 0
                                lastFpsTime = now
                            }
                        } catch (_: Throwable) {}
                    }
                }
                "clipboard" -> {
                    val text = j.get("text")?.asString ?: return
                    runOnUiThread {
                        try {
                            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("remote", text))
                            toast("收到剪贴板: " + text.take(20))
                        } catch (_: Throwable) {}
                    }
                }
                "device_info" -> {
                    val info = j.get("data")?.asString ?: return
                    runOnUiThread { showDialog("设备信息", info) }
                }
                "app_list" -> {
                    val list = j.get("data")?.asString ?: return
                    runOnUiThread { showDialog("已安装应用", list) }
                }
            }
        } catch (_: Throwable) {}
    }

    private fun showDialog(title: String, msg: String) {
        try {
            AlertDialog.Builder(this).setTitle(title).setMessage(msg)
                .setPositiveButton("确定", null).show()
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try { ws?.send("{\"type\":\"screen_stop\"}") } catch (_: Throwable) {}
        try { ws?.close() } catch (_: Throwable) {}
    }
}
