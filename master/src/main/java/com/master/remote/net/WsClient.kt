package com.master.remote.net

import android.os.Handler
import android.os.Looper
import okhttp3.*
import java.util.concurrent.TimeUnit

class WsClient(
    private val onOpen: () -> Unit,
    private val onMessage: (String) -> Unit,
    private val onClosed: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()
    private var ws: WebSocket? = null

    fun connect(ip: String, port: Int) {
        val url = "ws://$ip:$port/"
        val req = Request.Builder().url(url).build()
        ws = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(w: WebSocket, response: Response) { handler.post { onOpen() } }
            override fun onMessage(w: WebSocket, text: String) { handler.post { onMessage(text) } }
            override fun onClosed(w: WebSocket, code: Int, reason: String) { handler.post { onClosed(reason) } }
            override fun onFailure(w: WebSocket, t: Throwable, response: Response?) {
                handler.post { onClosed(t.message ?: "error") }
            }
        })
    }

    fun send(json: String) { ws?.send(json) }
    fun close() { try { ws?.close(1000, "bye") } catch (_: Exception) {} }
}
