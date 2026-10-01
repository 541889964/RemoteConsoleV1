package com.slave.remote.net

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.slave.remote.core.MainService
import com.slave.remote.core.RemoteControlService
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

class WsServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {

    companion object {
        private const val TAG = "WsServer"
        private val gson = Gson()
        var appContext: Context? = null
    }

    private val clients = CopyOnWriteArrayList<WebSocket>()
    private var broadcasting = false

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        Log.i(TAG, "client connected")
        clients.add(conn)
        try {
            val obj = JsonObject()
            obj.addProperty("type", "hello")
            obj.addProperty("name", "Slave V1")
            obj.addProperty("features", 25)
            conn.send(obj.toString())
        } catch (_: Throwable) {}
    }

    override fun onClose(conn: WebSocket?, c: Int, r: String?, remote: Boolean) {
        clients.remove(conn)
    }

    override fun onError(conn: WebSocket?, ex: Exception) { Log.e(TAG, "err", ex) }
    override fun onStart() { Log.i(TAG, "WsServer started on :8888") }

    /** 广播屏幕帧（base64 JPEG） */
    fun broadcastFrame(base64: String, w: Int, h: Int) {
        if (broadcasting) return
        broadcasting = true
        try {
            val obj = JsonObject()
            obj.addProperty("type", "frame")
            obj.addProperty("data", base64)
            obj.addProperty("w", w)
            obj.addProperty("h", h)
            val msg = obj.toString()
            for (c in clients) {
                try { if (c.isOpen) c.send(msg) } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {} finally {
            broadcasting = false
        }
    }

    override fun onMessage(conn: WebSocket, msg: String) {
        try {
            val j = gson.fromJson(msg, JsonObject::class.java)
            val type = j.get("type")?.asString ?: return
            val svc = RemoteControlService.instance

            when (type) {
                "tap" -> svc?.tap(j.get("x").asFloat, j.get("y").asFloat)
                "swipe" -> svc?.swipe(
                    j.get("x1").asFloat, j.get("y1").asFloat,
                    j.get("x2").asFloat, j.get("y2").asFloat,
                    j.get("duration")?.asLong ?: 300L)
                "longpress" -> svc?.longPress(
                    j.get("x").asFloat, j.get("y").asFloat,
                    j.get("duration")?.asLong ?: 600L)
                "back" -> svc?.back()
                "home" -> svc?.home()
                "recents" -> svc?.recents()
                "volume" -> svc?.volume(j.get("extra")?.asInt ?: 1)
                "mute" -> svc?.mute()
                "brightness" -> svc?.brightness(j.get("extra")?.asInt ?: 1)
                "lock" -> svc?.lock()
                "rotate" -> svc?.rotate()
                "clipboard" -> svc?.setClipboard(j.get("text")?.asString ?: "")
                "text" -> svc?.inputText(j.get("text")?.asString ?: "")
                "vibrate" -> svc?.vibrate(500L)
                "speak_demo" -> svc?.speak("收到主控端语音播报请求")
                "screenshot" -> {}
                "setQuality" -> {}
                "setFps" -> {}
                "screen_start" -> {}
                "screen_stop" -> {}

                "app_list" -> {
                    try {
                        val ctx = appContext ?: return
                        val pm = ctx.packageManager
                        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                        val sb = StringBuilder()
                        var n = 0
                        for (app in apps) {
                            sb.append(pm.getApplicationLabel(app).toString()).append("\n")
                            n++
                            if (n >= 30) break
                        }
                        val obj = JsonObject()
                        obj.addProperty("type", "app_list")
                        obj.addProperty("data", sb.toString())
                        conn.send(obj.toString())
                    } catch (_: Throwable) {}
                }

                "device_info" -> {
                    val s = if (svc != null) "${svc.screenW}×${svc.screenH}" else "?"
                    val info = "品牌: ${Build.BRAND}\n型号: ${Build.MODEL}\n" +
                            "厂商: ${Build.MANUFACTURER}\n系统: Android ${Build.VERSION.RELEASE}\n" +
                            "API: ${Build.VERSION.SDK_INT}\n分辨率: $s\n" +
                            "CPU: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "?"}"
                    val obj = JsonObject()
                    obj.addProperty("type", "device_info")
                    obj.addProperty("data", info)
                    conn.send(obj.toString())
                }

                "restart" -> {
                    try { MainService.instance?.stopCapture() } catch (_: Throwable) {}
                }

                "info" -> {
                    val obj = JsonObject()
                    obj.addProperty("type", "info")
                    obj.addProperty("w", svc?.screenW ?: 1080)
                    obj.addProperty("h", svc?.screenH ?: 1920)
                    conn.send(obj.toString())
                }

                "ping" -> conn.send("{\"type\":\"pong\"}")
            }
        } catch (e: Throwable) { Log.e(TAG, "onMessage", e) }
    }
}
