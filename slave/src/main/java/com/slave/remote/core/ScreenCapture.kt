package com.slave.remote.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.Log
import com.slave.remote.net.WsServer
import java.io.ByteArrayOutputStream

/**
 * 实时屏幕捕获
 * 每 300ms 捕获一帧，JPEG 压缩后通过 WebSocket 广播
 */
class ScreenCapture(
    private val ctx: Context,
    private val projection: MediaProjection,
    private val ws: WsServer?
) {
    companion object {
        private const val TAG = "ScreenCapture"
        private const val INTERVAL_MS = 300L  // 0.3 秒
        private var quality = 60
        private var enabled = false
    }

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var bgThread: HandlerThread? = null
    private var bgHandler: Handler? = null
    private var running = false
    private var lastCaptureTime = 0L

    fun start() {
        if (running) return
        running = true
        enabled = true
        try {
            val dm = ctx.resources.displayMetrics
            val w = dm.widthPixels
            val h = dm.heightPixels
            val dpi = dm.densityDpi

            bgThread = HandlerThread("screen-cap").also { it.start() }
            bgHandler = Handler(bgThread!!.looper)

            imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
            imageReader?.setOnImageAvailableListener({ reader ->
                val img = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val now = System.currentTimeMillis()
                    if (now - lastCaptureTime < INTERVAL_MS) {
                        img.close()
                        return@setOnImageAvailableListener
                    }
                    lastCaptureTime = now
                    processImage(img)
                } catch (e: Throwable) {
                    Log.e(TAG, "frame", e)
                } finally {
                    try { img.close() } catch (_: Throwable) {}
                }
            }, bgHandler)

            virtualDisplay = projection.createVirtualDisplay(
                "ScreenCapture",
                w, h, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, bgHandler
            )
        } catch (e: Throwable) { Log.e(TAG, "start", e) }
    }

    private fun processImage(img: Image) {
        try {
            val planes = img.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val w = img.width
            val h = img.height
            val rowPadding = rowStride - pixelStride * w

            val bmpW = w + rowPadding / pixelStride
            val bitmap = Bitmap.createBitmap(bmpW, h, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)

            val cropped: Bitmap = if (rowPadding == 0) bitmap
            else Bitmap.createBitmap(bitmap, 0, 0, w, h)

            val stream = ByteArrayOutputStream(2 * 1024 * 1024)
            cropped.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val bytes = stream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            // 通过 WebSocket 广播
            ws?.broadcastFrame(base64, w, h)

            if (cropped !== bitmap) cropped.recycle()
            bitmap.recycle()
        } catch (e: Throwable) { Log.e(TAG, "process", e) }
    }

    fun stop() {
        running = false
        enabled = false
        try { virtualDisplay?.release() } catch (_: Throwable) {}
        try { imageReader?.close() } catch (_: Throwable) {}
        try { projection.stop() } catch (_: Throwable) {}
        try { bgThread?.quitSafely() } catch (_: Throwable) {}
        virtualDisplay = null; imageReader = null; bgThread = null; bgHandler = null
    }

    fun setQuality(q: Int) { quality = q.coerceIn(20, 95) }
    fun isRunning(): Boolean = running
}
