package com.slave.remote

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.slave.remote.core.MainService
import com.slave.remote.util.Compat
import android.content.Context

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Compat.startForegroundService(this, MainService::class.java)

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        findViewById<View>(R.id.btnAccessibility)?.setOnClickListener {
            try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (_: Exception) {}
        }

        findViewById<View>(R.id.btnBatteryOpt)?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    i.data = Uri.parse("package:$packageName")
                    startActivity(i)
                } catch (_: Exception) {
                    try { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                    catch (_: Exception) {}
                }
            }
        }

        findViewById<View>(R.id.btnAutoStart)?.setOnClickListener {
            val intents = listOf(
                Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            )
            for (i in intents) {
                try { startActivity(i); return@setOnClickListener } catch (_: Exception) {}
            }
            try {
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                i.data = Uri.parse("package:$packageName")
                startActivity(i)
            } catch (_: Exception) {}
        }

        updateStatus()
    }

    override fun onResume() { super.onResume(); updateStatus() }

    private fun updateStatus() {
        try {
            val tv = findViewById<TextView>(R.id.tvStatus) ?: return
            val accOn = isAccessibilityEnabled()
            val batteryOk = isBatteryOptimized()
            tv.text = buildString {
                append("版本: V1\n")
                append("无障碍: ").append(if (accOn) "✓ 已开启" else "✗ 未开启")
                append("\n电池豁免: ").append(if (batteryOk) "✓ 已豁免" else "✗ 未豁免")
                append("\n系统: Android ").append(Build.VERSION.RELEASE)
                append(" (API ").append(Build.VERSION.SDK_INT).append(")")
                append("\n厂商: ").append(Build.MANUFACTURER)
                append("\n端口: 8888")
            }
        } catch (_: Exception) {}
    }

    private fun isAccessibilityEnabled(): Boolean {
        return try {
            val service = "$packageName/com.slave.remote.core.RemoteControlService"
            val enabled = Settings.Secure.getString(contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            enabled.contains(service)
        } catch (_: Exception) { false }
    }

    private fun isBatteryOptimized(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                pm.isIgnoringBatteryOptimizations(packageName)
            } else true
        } catch (_: Exception) { false }
    }
}
