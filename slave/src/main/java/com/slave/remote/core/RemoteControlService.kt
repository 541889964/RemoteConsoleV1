package com.slave.remote.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class RemoteControlService : AccessibilityService() {
    companion object {
        @Volatile var instance: RemoteControlService? = null
            private set
    }
    var screenW = 1080; private set
    var screenH = 1920; private set
    private var tts: TextToSpeech? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        try {
            val dm = resources.displayMetrics
            screenW = dm.widthPixels
            screenH = dm.heightPixels
        } catch (_: Throwable) {}
        try {
            tts = TextToSpeech(this, null)
        } catch (_: Throwable) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        try { tts?.shutdown() } catch (_: Throwable) {}
    }

    fun tap(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            val p = Path().apply { moveTo(x, y) }
            val g = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(p, 0, 50)).build()
            dispatchGesture(g, null, null)
        } catch (_: Throwable) {}
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            val p = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
            val g = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(p, 0, ms)).build()
            dispatchGesture(g, null, null)
        } catch (_: Throwable) {}
    }

    fun longPress(x: Float, y: Float, ms: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        try {
            val p = Path().apply { moveTo(x, y) }
            val g = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(p, 0, ms)).build()
            dispatchGesture(g, null, null)
        } catch (_: Throwable) {}
    }

    fun back() { try { performGlobalAction(GLOBAL_ACTION_BACK) } catch (_: Throwable) {} }
    fun home() { try { performGlobalAction(GLOBAL_ACTION_HOME) } catch (_: Throwable) {} }
    fun recents() { try { performGlobalAction(GLOBAL_ACTION_RECENTS) } catch (_: Throwable) {} }

    fun volume(delta: Int) {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, if (delta > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, 0)
        } catch (_: Throwable) {}
    }

    fun mute() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
        } catch (_: Throwable) {}
    }

    fun brightness(delta: Int) {
        try {
            val resolver = contentResolver
            val cur = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            val target = (cur + delta * 20).coerceIn(10, 255)
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, target)
        } catch (_: Throwable) {}
    }

    fun lock() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
        } catch (_: Throwable) {}
    }

    fun setClipboard(text: String) {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("remote", text))
        } catch (_: Throwable) {}
    }

    fun inputText(text: String) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
            val root = rootInActiveWindow ?: return
            val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } catch (_: Throwable) {}
    }

    // 新增功能：震动
    fun vibrate(ms: Long) {
        try {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(android.os.VibrationEffect.createOneShot(ms, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (_: Throwable) {}
    }

    // 新增功能：语音播报
    fun speak(text: String) {
        try {
            tts?.setLanguage(Locale.CHINA)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "remote")
        } catch (_: Throwable) {}
    }

    // 新增功能：屏幕旋转
    fun rotate() {
        try {
            val cur = Settings.System.getInt(contentResolver, Settings.System.USER_ROTATION, 0)
            Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, (cur + 1) % 4)
        } catch (_: Throwable) {}
    }
}
