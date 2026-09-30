package com.slave.remote.util

import android.content.Context
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

object CrashHandler {
    private const val TAG = "SlaveCrash"

    fun install(ctx: Context) {
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                Log.e(TAG, "Uncaught on ${t.name}: ${sw.toString()}")
            } catch (_: Exception) {}
            try { def?.uncaughtException(t, e) } catch (_: Exception) {}
        }
    }
}
