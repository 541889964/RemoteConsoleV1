package com.slave.remote.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class RemoteControlService : AccessibilityService() {
    companion object {
        @Volatile var instance: RemoteControlService? = null
            private set
    }
    var screenW = 1080; private set
    var screenH = 1920; private set

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        try {
            val dm = resources.displayMetrics
            screenW = dm.widthPixels
            screenH = dm.heightPixels
        } catch (_: Throwable) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { super.onDestroy(); instance = null }

    fun tap(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            val p = Path().apply { moveTo(x, y) }
            val g = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(p, 0, 50)).build()
            dispatchGesture(g, null, null)
        } catch (_: Throwable) {}
    }
    fun longPress(x: Float, y: Float, ms: Long = 600L) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            val p = Path().apply { moveTo(x, y) }
            val g = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(p, 0, ms)).build()
            dispatchGesture(g, null, null)
        } catch (_: Throwable) {}
    }
    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long = 300L) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            val p = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
            val g = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(p, 0, ms)).build()
            dispatchGesture(g, null, null)
        } catch (_: Throwable) {}
    }
    fun back() { try { performGlobalAction(GLOBAL_ACTION_BACK) } catch (_: Throwable) {} }
    fun home() { try { performGlobalAction(GLOBAL_ACTION_HOME) } catch (_: Throwable) {} }
    fun recents() { try { performGlobalAction(GLOBAL_ACTION_RECENTS) } catch (_: Throwable) {} }
}
