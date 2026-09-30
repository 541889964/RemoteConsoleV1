package com.slave.remote.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object Compat {

    fun serviceType(): Int =
        if (Build.VERSION.SDK_INT >= 29)
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        else 0

    fun startForegroundService(ctx: Context, serviceClass: Class<*>) {
        val i = Intent(ctx, serviceClass)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        } catch (e: Exception) {
            try { ctx.startService(i) } catch (_: Exception) {}
        }
    }

    fun startForegroundCompat(service: Service, id: Int, notification: Notification, type: Int) {
        if (Build.VERSION.SDK_INT >= 29) {
            service.startForeground(id, notification, type)
        } else {
            service.startForeground(id, notification)
        }
    }

    fun ensureChannel(ctx: Context, id: String, name: String, importance: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(id) == null) {
                val ch = NotificationChannel(id, name, importance)
                ch.setShowBadge(false)
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun buildNotification(ctx: Context, channelId: String, title: String, text: String): Notification {
        ensureChannel(ctx, channelId, title, NotificationManager.IMPORTANCE_MIN)
        return NotificationCompat.Builder(ctx, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
