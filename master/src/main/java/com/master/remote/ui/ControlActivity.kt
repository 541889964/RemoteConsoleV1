package com.master.remote.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.master.remote.R
import com.master.remote.net.WsClient

class ControlActivity : AppCompatActivity() {

    private lateinit var ws: WsClient
    private lateinit var screenView: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var tvTitle: TextView
    private val gson = Gson()

    private var screenW = 1080; private var screenH = 1920
    private var viewW = 0; private var viewH = 0
    private var downX = 0f; private var downY = 0f; private var downTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_control) } catch (_: Exception) { finish(); return }

        val ip = intent.getStringExtra("ip") ?: run { finish(); return }
        val port = intent.getIntExtra("port", 8888)
        val name = intent.getStringExtra("name") ?: "Device"

        safe {
            tvTitle = findViewById(R.id.tvTitle)
            tvStatus = findViewById(R.id.tvStatus)
            screenView = findViewById(R.id.screenView)
            tvTitle.text = name
            tvStatus.text = "连接中..."
        }

        ws = WsClient(
            onOpen = { runOnUiThread { tvStatus?.text = "已连接 $ip:$port" } },
            onMessage = { msg -> handleMessage(msg) },
            onClosed = { reason -> runOnUiThread { tvStatus?.text = "已断开: $reason" } })
        try { ws.connect(ip, port) } catch (_: Exception) {}

        safe {
            screenView.setOnTouchListener { _, ev ->
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = ev.x; downY = ev.y
                        downTime = System.currentTimeMillis()
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val dx = Math.abs(ev.x - downX); val dy = Math.abs(ev.y - downY)
                        val dt = System.currentTimeMillis() - downTime
                        if (dx < 20 && dy < 20) {
                            if (dt > 600) sendLongPress(ev.x, ev.y) else sendTap(ev.x, ev.y)
                        } else sendSwipe(downX, downY, ev.x, ev.y, dt)
                        true
                    }
                    else -> true
                }
            }
        }

        safe { findViewById<View>(R.id.btnBack)?.setOnClickListener { sendCmd("back") } }
        safe { findViewById<View>(R.id.btnHome)?.setOnClickListener { sendCmd("home") } }
        safe { findViewById<View>(R.id.btnRecents)?.setOnClickListener { sendCmd("recents") } }
        safe { findViewById<View>(R.id.btnClose)?.setOnClickListener { finish() } }

        Handler(Looper.getMainLooper()).postDelayed({
            try { ws.send("""{"type":"info"}""") } catch (_: Exception) {}
        }, 800L)
    }

    private fun toDeviceX(vx: Float): Float = if (viewW == 0) vx else vx / viewW * screenW
    private fun toDeviceY(vy: Float): Float = if (viewH == 0) vy else vy / viewH * screenH

    private fun sendTap(vx: Float, vy: Float) {
        try { ws.send("""{"type":"tap","x":${toDeviceX(vx)},"y":${toDeviceY(vy)}}""") }
        catch (_: Exception) {}
    }
    private fun sendLongPress(vx: Float, vy: Float) {
        try { ws.send("""{"type":"longpress","x":${toDeviceX(vx)},"y":${toDeviceY(vy)},"duration":800}""") }
        catch (_: Exception) {}
    }
    private fun sendSwipe(x1: Float, y1: Float, x2: Float, y2: Float, dt: Long) {
        try {
            ws.send("""{"type":"swipe","x1":${toDeviceX(x1)},"y1":${toDeviceY(y1)},"x2":${toDeviceX(x2)},"y2":${toDeviceY(y2)},"duration":${maxOf(dt, 100)}}""")
        } catch (_: Exception) {}
    }
    private fun sendCmd(type: String) { try { ws.send("""{"type":"$type"}""") } catch (_: Exception) {} }

    private fun handleMessage(msg: String) {
        try {
            val json = gson.fromJson(msg, JsonObject::class.java)
            when (json.get("type")?.asString) {
                "info" -> {
                    screenW = json.get("w")?.asInt ?: screenW
                    screenH = json.get("h")?.asInt ?: screenH
                }
                "frame" -> {
                    val data = json.get("data")?.asString ?: return
                    val bytes = Base64.decode(data, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
                    runOnUiThread {
                        safe {
                            viewW = screenView.width
                            viewH = screenView.height
                            screenView.setImageBitmap(bmp)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private inline fun safe(block: () -> Unit) { try { block() } catch (_: Exception) {} }

    override fun onDestroy() { super.onDestroy(); try { ws.close() } catch (_: Exception) {} }
}
