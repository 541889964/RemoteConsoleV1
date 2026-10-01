package com.slave.remote.core

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.slave.remote.net.WsServer

class MainService : Service() {

    companion object {
        private const val TAG = "MainService"
        private const val NOTIF_ID = 1001
        private const val CHANNEL = "svc"
        @Volatile var instance: MainService? = null
            private set
    }

    private var ws: WsServer? = null
    private var capture: ScreenCapture? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        WsServer.appContext = applicationContext

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (nm.getNotificationChannel(CHANNEL) == null) {
                    nm.createNotificationChannel(NotificationChannel(
                        CHANNEL, "远程服务", NotificationManager.IMPORTANCE_MIN))
                }
            }
            val notif = NotificationCompat.Builder(this, CHANNEL)
                .setContentTitle("Remote Slave V1")
                .setContentText("25 大功能 · 等待连接 · 端口 8888")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()
            startForeground(NOTIF_ID, notif)
        } catch (e: Throwable) { Log.e(TAG, "fg", e) }

        try {
            ws = WsServer(8888)
            ws?.start()
        } catch (e: Throwable) { Log.e(TAG, "ws", e) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_PROJECTION") {
            val code = intent.getIntExtra("resultCode", -1)
            val data = intent.getParcelableExtra<Intent>("data")
            if (code != -1 && data != null) {
                startCapture(code, data)
            }
        }
        return START_STICKY
    }

    private fun startCapture(code: Int, data: Intent) {
        try {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = mpm.getMediaProjection(code, data)
            if (projection == null) return
            capture = ScreenCapture(applicationContext, projection, ws)
            capture?.start()
        } catch (e: Throwable) { Log.e(TAG, "capture", e) }
    }

    fun stopCapture() {
        try { capture?.stop(); capture = null } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        try { capture?.stop() } catch (_: Throwable) {}
        try { ws?.stop() } catch (_: Throwable) {}
        instance = null
        super.onDestroy()
    }
}
