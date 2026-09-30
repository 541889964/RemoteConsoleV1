package com.slave.remote

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.slave.remote.util.CrashHandler

class SlaveApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try { MultiDex.install(this) } catch (_: Exception) {}
    }

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}
