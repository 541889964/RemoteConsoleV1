package com.master.remote

import android.app.Application
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class MasterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
    }

    private fun installCrashHandler() {
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
                val f = File(Environment.getExternalStorageDirectory(), "RemoteConsoleCrash-$ts.txt")
                f.writeText("Thread: ${t.name}\n\n${sw.toString()}")
                Log.e("MasterCrash", "saved: ${f.absolutePath}")
            } catch (_: Throwable) {}
            def?.uncaughtException(t, e)
        }
    }
}
