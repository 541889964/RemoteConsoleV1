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
    private val h = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()
    private var ws: WebSocket? = null

    fun connect(ip: String, port: Int) {
        try {
            val req = Request.Builder().url("ws://$ip:$port/").build()
            ws = client.newWebSocket(req, object : WebSocketListener() {
                override fun onOpen(w: WebSocket, r: Response) { h.post { try { onOpen() } catch (_: Throwable) {} } }
                override fun onMessage(w: WebSocket, t: String) { h.post { try { onMessage(t) } catch (_: Throwable) {} } }
                override fun onClosed(w: WebSocket, c: Int, r: String) { h.post { try { onClosed(r) } catch (_: Throwable) {} } }
                override fun onFailure(w: WebSocket, t: Throwable, r: Response?) {
                    h.post { try { onClosed(t.message ?: "error") } catch (_: Throwable) {} }
                }
            })
        } catch (_: Throwable) {}
    }

    fun send(j: String) { try { ws?.send(j) } catch (_: Throwable) {} }
    fun close() { try { ws?.close(1000, "bye") } catch (_: Throwable) {} }
}
