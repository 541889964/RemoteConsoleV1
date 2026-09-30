package com.master.remote.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
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
    private val TAG = "Control"
    private var ws: WsClient? = null
    private var screenView: ImageView? = null
    private var tvStatus: TextView? = null
    private val gson = Gson()

    private var screenW = 1080
    private var screenH = 1920
    private var viewW = 0
    private var viewH = 0
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_control) } catch (e: Throwable) {
            Log.e(TAG, "setContentView", e); finish(); return
        }

        val ip = intent.getStringExtra("ip") ?: run { finish(); return }
        val port = intent.getIntExtra("port", 8888)
        val name = intent.getStringExtra("name") ?: "Device"

        try {
            screenView = findViewById(R.id.screenView)
            tvStatus = findViewById(R.id.tvStatus)
            findViewById<TextView>(R.id.tvTitle).text = name
            tvStatus?.text = "连接中..."
        } catch (e: Throwable) { Log.e(TAG, "findView", e) }

        ws = WsClient(
            onOpen = { runOnUiThread { tvStatus?.text = "已连接" } },
            onMessage = { msg -> handleMessage(msg) },
            onClosed = { r -> runOnUiThread { tvStatus?.text = "断开: $r" } }
        )
        try { ws?.connect(ip, port) } catch (e: Throwable) { Log.e(TAG, "connect", e) }

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
                                if (dt > 600) sendLongPress(ev.x, ev.y) else sendTap(ev.x, ev.y)
                            } else sendSwipe(downX, downY, ev.x, ev.y, dt)
                        }
                    }
                } catch (_: Throwable) {}
                true
            }
        } catch (_: Throwable) {}

        try { findViewById<View>(R.id.btnBack).setOnClickListener { sendCmd("back") } } catch (_: Throwable) {}
        try { findViewById<View>(R.id.btnHome).setOnClickListener { sendCmd("home") } } catch (_: Throwable) {}
        try { findViewById<View>(R.id.btnRecents).setOnClickListener { sendCmd("recents") } } catch (_: Throwable) {}
        try { findViewById<View>(R.id.btnClose).setOnClickListener { finish() } } catch (_: Throwable) {}

        Handler(Looper.getMainLooper()).postDelayed({
            try { ws?.send("""{"type":"info"}""") } catch (_: Throwable) {}
        }, 800)
    }

    private fun toX(vx: Float): Float = if (viewW == 0) vx else vx / viewW * screenW
    private fun toY(vy: Float): Float = if (viewH == 0) vy else vy / viewH * screenH

    private fun sendTap(x: Float, y: Float) {
        try { ws?.send("""{"type":"tap","x":${toX(x)},"y":${toY(y)}}""") } catch (_: Throwable) {}
    }
    private fun sendLongPress(x: Float, y: Float) {
        try { ws?.send("""{"type":"longpress","x":${toX(x)},"y":${toY(y)},"duration":800}""") } catch (_: Throwable) {}
    }
    private fun sendSwipe(x1: Float, y1: Float, x2: Float, y2: Float, dt: Long) {
        try { ws?.send("""{"type":"swipe","x1":${toX(x1)},"y1":${toY(y1)},"x2":${toX(x2)},"y2":${toY(y2)},"duration":${maxOf(dt, 100)}}""") } catch (_: Throwable) {}
    }
    private fun sendCmd(t: String) { try { ws?.send("""{"type":"$t"}""") } catch (_: Throwable) {} }

    private fun handleMessage(msg: String) {
        try {
            val j = gson.fromJson(msg, JsonObject::class.java)
            when (j.get("type")?.asString) {
                "info" -> {
                    screenW = j.get("w")?.asInt ?: screenW
                    screenH = j.get("h")?.asInt ?: screenH
                }
                "frame" -> {
                    val data = j.get("data")?.asString ?: return
                    val bytes = Base64.decode(data, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
                    runOnUiThread {
                        try {
                            viewW = screenView?.width ?: 0
                            viewH = screenView?.height ?: 0
                            screenView?.setImageBitmap(bmp)
                        } catch (_: Throwable) {}
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try { ws?.close() } catch (_: Throwable) {}
    }
}
