package com.slave.remote.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.slave.remote.util.Compat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val a = intent.action ?: return
        Log.i("BootReceiver", "received: $a")

        val valid = a == Intent.ACTION_BOOT_COMPLETED ||
                a == "android.intent.action.QUICKBOOT_POWERON" ||
                a == "com.htc.intent.action.QUICKBOOT_POWERON" ||
                a == Intent.ACTION_MY_PACKAGE_REPLACED ||
                a == Intent.ACTION_PACKAGE_REPLACED ||
                a == "com.slave.remote.KEEP_ALIVE"
        if (!valid) return

        try {
            Compat.startForegroundService(context, MainService::class.java)
        } catch (e: Exception) { Log.e("BootReceiver", "start", e) }

        scheduleRetry(context, 60_000L)
    }

    private fun scheduleRetry(ctx: Context, ms: Long) {
        try {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
            val pi = PendingIntent.getBroadcast(ctx, 0,
                Intent(ctx, BootReceiver::class.java).apply {
                    action = "com.slave.remote.KEEP_ALIVE"
                }, flags)
            val at = SystemClock.elapsedRealtime() + ms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi)
            } else {
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi)
            }
        } catch (e: Exception) { Log.e("BootReceiver", "alarm", e) }
    }
}

class KeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try { Compat.startForegroundService(context, MainService::class.java) } catch (_: Exception) {}
    }
}
