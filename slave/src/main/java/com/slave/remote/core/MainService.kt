package com.slave.remote.core

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.slave.remote.net.WsServer
import com.slave.remote.util.Compat

class MainService : Service() {

    companion object {
        private const val TAG = "MainService"
        private const val NOTIF_ID = 1002
        private const val CHANNEL = "main_service"
    }

    private var wsServer: WsServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val notif = Compat.buildNotification(this, CHANNEL,
                "Remote Slave V1", "等待主控连接 · :8888")
            Compat.startForegroundCompat(this, NOTIF_ID, notif, Compat.serviceType())
        } catch (e: Exception) { Log.e(TAG, "foreground", e) }

        try {
            wsServer = WsServer(8888).also { it.start() }
            Log.i(TAG, "WS server started on :8888")
        } catch (e: Exception) { Log.e(TAG, "WS start", e) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        try { wsServer?.stop() } catch (_: Exception) {}
        wsServer = null
        super.onDestroy()
    }
}
