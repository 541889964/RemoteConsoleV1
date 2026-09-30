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
        try { conn.send("""{"type":"hello","name":"Remote Slave V1"}""") } catch (_: Exception) {}
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {}

    override fun onMessage(conn: WebSocket, message: String) {
        try {
            val json = gson.fromJson(message, JsonObject::class.java)
            val type = json.get("type")?.asString ?: return
            val svc = RemoteControlService.instance ?: run {
                conn.send("""{"error":"accessibility not ready"}""")
                return
            }
            when (type) {
                "tap" -> svc.tap(json.get("x").asFloat, json.get("y").asFloat)
                "swipe" -> svc.swipe(
                    json.get("x1").asFloat, json.get("y1").asFloat,
                    json.get("x2").asFloat, json.get("y2").asFloat,
                    json.get("duration")?.asLong ?: 300L)
                "longpress" -> svc.longPress(
                    json.get("x").asFloat, json.get("y").asFloat,
                    json.get("duration")?.asLong ?: 600L)
                "back" -> svc.back()
                "home" -> svc.home()
                "recents" -> svc.recents()
                "ping" -> conn.send("""{"type":"pong"}""")
                "info" -> conn.send("""{"type":"info","w":${svc.screenW},"h":${svc.screenH}}""")
            }
        } catch (e: Exception) { Log.e(TAG, "onMessage", e) }
    }

    override fun onError(conn: WebSocket?, ex: Exception) { Log.e(TAG, "onError", ex) }
    override fun onStart() { Log.i(TAG, "WsServer started") }
}
