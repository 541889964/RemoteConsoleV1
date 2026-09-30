package com.slave.remote.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class RemoteControlService : AccessibilityService() {

    companion object {
        private const val TAG = "RCS"
        private const val THROTTLE_MS = 100L
        private const val MAX_QUEUE = 64

        @Volatile var instance: RemoteControlService? = null
            private set
    }

    private val handler = Handler(Looper.getMainLooper())
    private val throttleMap = mutableMapOf<Int, Long>()
    private val queue = ConcurrentLinkedQueue<GestureTask>()
    private val processing = AtomicBoolean(false)

    var screenW = 1080; private set
    var screenH = 1920; private set

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        try {
            val dm = resources.displayMetrics
            screenW = dm.widthPixels
            screenH = dm.heightPixels
        } catch (_: Exception) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        try {
            val now = SystemClock.uptimeMillis()
            if (now - (throttleMap[event.eventType] ?: 0L) < THROTTLE_MS) return
            throttleMap[event.eventType] = now
        } catch (_: Exception) {}
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        queue.clear(); throttleMap.clear()
    }

    override fun onLowMemory() { throttleMap.clear() }

    fun tap(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        enqueue(GestureTask(Path().apply { moveTo(x, y) }, 0L, 50L))
    }

    fun longPress(x: Float, y: Float, ms: Long = 600L) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        enqueue(GestureTask(Path().apply { moveTo(x, y) }, 0L, ms))
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long = 300L) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        enqueue(GestureTask(Path().apply { moveTo(x1, y1); lineTo(x2, y2) }, 0L, ms))
    }

    fun back()    { performGlobalAction(GLOBAL_ACTION_BACK) }
    fun home()    { performGlobalAction(GLOBAL_ACTION_HOME) }
    fun recents() { performGlobalAction(GLOBAL_ACTION_RECENTS) }

    private data class GestureTask(val path: Path, val start: Long, val dur: Long)

    private fun enqueue(t: GestureTask) {
        if (queue.size >= MAX_QUEUE) queue.poll()
        queue.offer(t); process()
    }

    private fun process() {
        if (!processing.compareAndSet(false, true)) return
        val next = queue.poll()
        if (next == null) { processing.set(false); return }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            processing.set(false); return
        }
        try {
            val g = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(next.path, next.start, next.dur))
                .build()
            val cb = object : GestureResultCallback() {
                override fun onCompleted(desc: GestureDescription?) { processing.set(false); process() }
                override fun onCancelled(desc: GestureDescription?) {
                    queue.offer(next); processing.set(false)
                    handler.postDelayed({ process() }, 50L)
                }
            }
            val ok = dispatchGesture(g, cb, handler)
            if (!ok) { processing.set(false); handler.postDelayed({ process() }, 50L) }
        } catch (e: Exception) {
            Log.e(TAG, "process", e); processing.set(false)
        }
    }
}
