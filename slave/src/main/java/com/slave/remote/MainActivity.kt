package com.slave.remote

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
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

class MainActivity : AppCompatActivity() {

    private val REQ_PROJECTION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_main) } catch (_: Throwable) { finish(); return }

        // 启动服务
        try {
            val i = Intent(this, MainService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
            else startService(i)
        } catch (_: Throwable) {}

        // 请求通知权限
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                try {
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
                } catch (_: Throwable) {}
            }
        }

        val pkgName: String = applicationContext.packageName

        try {
            (findViewById(R.id.btnAcc) as? View)?.setOnClickListener {
                try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (_: Throwable) {}
            }
            (findViewById(R.id.btnScreen) as? View)?.setOnClickListener {
                requestProjection()
            }
            (findViewById(R.id.btnBat) as? View)?.setOnClickListener {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        i.data = Uri.parse("package:" + pkgName)
                        startActivity(i)
                    }
                } catch (_: Throwable) {
                    try { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                    catch (_: Throwable) {}
                }
            }
            (findViewById(R.id.btnAutostart) as? View)?.setOnClickListener {
                val intents = listOf(
                    Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                    Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                    Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                    Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                )
                for (i in intents) {
                    try { startActivity(i); return@setOnClickListener } catch (_: Throwable) {}
                }
                try {
                    val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    i.data = Uri.parse("package:" + pkgName)
                    startActivity(i)
                } catch (_: Throwable) {}
            }
            (findViewById(R.id.btnStatus) as? View)?.setOnClickListener { updateStatus() }
        } catch (_: Throwable) {}

        updateStatus()
    }

    private fun requestProjection() {
        try {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mpm.createScreenCaptureIntent(), REQ_PROJECTION)
        } catch (_: Throwable) {}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PROJECTION && resultCode == Activity.RESULT_OK && data != null) {
            try {
                val intent = Intent(this, MainService::class.java)
                intent.putExtra("resultCode", resultCode)
                intent.putExtra("data", data)
                intent.action = "START_PROJECTION"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
                else startService(intent)
            } catch (_: Throwable) {}
        }
    }

    override fun onResume() { super.onResume(); updateStatus() }

    private fun updateStatus() {
        try {
            val tv = findViewById(R.id.tvStatus) as? TextView ?: return
            val acc = isAccOn()
            val bat = isBatOk()
            tv.text = "版本: V1 · 25 大功能\n" +
                    "无障碍: " + (if (acc) "✓ 已开启" else "✗ 未开启") + "\n" +
                    "电池豁免: " + (if (bat) "✓ 已豁免" else "✗ 未豁免") + "\n" +
                    "系统: Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n" +
                    "厂商: " + Build.MANUFACTURER + "\n" +
                    "监听: 8888"
        } catch (_: Throwable) {}
    }

    private fun isAccOn(): Boolean = try {
        val svc = applicationContext.packageName + "/com.slave.remote.core.RemoteControlService"
        Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.contains(svc) ?: false
    } catch (_: Throwable) { false }

    private fun isBatOk(): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(applicationContext.packageName)
        } else true
    } catch (_: Throwable) { false }
}
