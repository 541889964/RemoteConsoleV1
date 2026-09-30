package com.slave.remote

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class SlaveApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try { MultiDex.install(this) } catch (_: Throwable) {}
    }

    override fun onCreate() {
        super.onCreate()
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
                val f = File(Environment.getExternalStorageDirectory(), "SlaveCrash-$ts.txt")
                f.writeText("Thread: ${t.name}\n\n${sw.toString()}")
                Log.e("SlaveCrash", "saved: ${f.absolutePath}")
            } catch (_: Throwable) {}
            def?.uncaughtException(t, e)
        }
    }
}
