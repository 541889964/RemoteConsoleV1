package com.slave.remote.net

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.slave.remote.core.RemoteControlService
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

class WsServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {
    companion object {
        private const val TAG = "WsServer"
        private val gson = Gson()
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        try { conn.send("""{"type":"hello","name":"Slave V1"}""") } catch (_: Throwable) {}
    }
    override fun onClose(conn: WebSocket?, c: Int, r: String?, remote: Boolean) {}
    override fun onError(conn: WebSocket?, ex: Exception) { Log.e(TAG, "err", ex) }
    override fun onStart() { Log.i(TAG, "started") }

    override fun onMessage(conn: WebSocket, msg: String) {
        try {
            val j = gson.fromJson(msg, JsonObject::class.java)
            val type = j.get("type")?.asString ?: return
            val svc = RemoteControlService.instance ?: run {
                conn.send("""{"error":"no service"}"""); return
            }
            when (type) {
                "tap" -> svc.tap(j.get("x").asFloat, j.get("y").asFloat)
                "swipe" -> svc.swipe(j.get("x1").asFloat, j.get("y1").asFloat,
                    j.get("x2").asFloat, j.get("y2").asFloat,
                    j.get("duration")?.asLong ?: 300L)
                "longpress" -> svc.longPress(j.get("x").asFloat, j.get("y").asFloat,
                    j.get("duration")?.asLong ?: 600L)
                "back" -> svc.back()
                "home" -> svc.home()
                "recents" -> svc.recents()
                "ping" -> conn.send("""{"type":"pong"}""")
                "info" -> conn.send("""{"type":"info","w":${svc.screenW},"h":${svc.screenH}}""")
            }
        } catch (_: Throwable) {}
    }
}
