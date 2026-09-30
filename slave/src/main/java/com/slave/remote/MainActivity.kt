package com.slave.remote

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.slave.remote.core.MainService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_main) } catch (e: Throwable) {
            Log.e("SlaveMain", "setContentView", e); finish(); return
        }

        // 启动服务（带 try-catch）
        try {
            val i = Intent(this, MainService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
            else startService(i)
        } catch (e: Throwable) { Log.e("SlaveMain", "startService", e) }

        try {
            findViewById<android.view.View>(R.id.btnAccessibility).setOnClickListener {
                try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (_: Throwable) {}
            }
            findViewById<android.view.View>(R.id.btnBattery).setOnClickListener {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                } catch (_: Throwable) {}
            }
            findViewById<android.view.View>(R.id.btnStatus).setOnClickListener { updateStatus() }
        } catch (e: Throwable) { Log.e("SlaveMain", "buttons", e) }

        updateStatus()
    }

    override fun onResume() { super.onResume(); updateStatus() }

    private fun updateStatus() {
        try {
            val tv = findViewById<TextView>(R.id.tvStatus)
            val acc = isAccessibilityOn()
            tv.text = "版本: V1\n" +
                    "无障碍: ${if (acc) "✓ 已开启" else "✗ 未开启"}\n" +
                    "系统: Android ${Build.VERSION.RELEASE}\n" +
                    "API: ${Build.VERSION.SDK_INT}\n" +
                    "端口: 8888\n" +
                    "厂商: ${Build.MANUFACTURER}"
        } catch (_: Throwable) {}
    }

    private fun isAccessibilityOn(): Boolean {
        return try {
            val svc = "$packageName/com.slave.remote.core.RemoteControlService"
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.contains(svc) ?: false
        } catch (_: Throwable) { false }
    }
}
