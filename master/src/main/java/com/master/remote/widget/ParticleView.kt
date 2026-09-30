package com.master.remote.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class ParticleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private data class P(var x: Float, var y: Float, val r: Float,
                         val sp: Float, val al: Int, val cl: Int)

    private val list = mutableListOf<P>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var init = false
    private val colors = intArrayOf(
        Color.parseColor("#4A6BFF"), Color.parseColor("#7A5BFF"),
        Color.parseColor("#5A6BFF"), Color.parseColor("#E94560"))

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        if (!init) {
            repeat(45) {
                list.add(P(Random.nextFloat() * w, Random.nextFloat() * h,
                    Random.nextFloat() * 3.5f + 1f,
                    Random.nextFloat() * 0.9f + 0.3f,
                    Random.nextInt(70, 220), colors.random()))
            }
            init = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (p in list) {
            paint.color = p.cl; paint.alpha = p.al
            canvas.drawCircle(p.x, p.y, p.r, paint)
            p.y -= p.sp
            if (p.y < -10f) {
                p.y = height + 10f
                p.x = Random.nextFloat() * width
            }
        }
        if (isShown) postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); list.clear() }
}
