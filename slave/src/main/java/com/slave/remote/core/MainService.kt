package com.slave.remote.core

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.slave.remote.net.WsServer

class MainService : Service() {
    private var ws: WsServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try { startForeground(1001, buildNotif()) } catch (e: Throwable) { Log.e("MainService", "fg", e) }
        try {
            ws = WsServer(8888)
            ws?.start()
        } catch (e: Throwable) { Log.e("MainService", "ws", e) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        try { ws?.stop() } catch (_: Throwable) {}
        super.onDestroy()
    }

    private fun buildNotif(): Notification {
        val ch = "main_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(ch) == null) {
                nm.createNotificationChannel(NotificationChannel(ch, "服务", NotificationManager.IMPORTANCE_MIN))
            }
        }
        return NotificationCompat.Builder(this, ch)
            .setContentTitle("Remote Slave V1")
            .setContentText("等待连接 · :8888")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }
}
