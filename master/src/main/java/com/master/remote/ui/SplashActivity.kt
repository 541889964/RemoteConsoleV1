package com.master.remote.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.master.remote.R
import com.master.remote.widget.ParticleView

class SplashActivity : AppCompatActivity() {

    private lateinit var particle: ParticleView
    private lateinit var glow: View
    private lateinit var ring: View
    private lateinit var logo: View
    private lateinit var tvBrand1: TextView
    private lateinit var tvBrand2: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvLoading: TextView
    private lateinit var tvVersion: TextView
    private lateinit var divider: View
    private lateinit var progress: ProgressBar
    private lateinit var dot1: View; private lateinit var dot2: View; private lateinit var dot3: View

    private val handler = Handler(Looper.getMainLooper())
    private val anims = mutableListOf<ValueAnimator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_splash) } catch (_: Exception) { goMain(); return }
        try { bind() } catch (_: Exception) { goMain(); return }
        try { orchestrate() } catch (_: Exception) {}
        handler.postDelayed({ goMain() }, 20_000L)
    }

    private fun goMain() {
        if (isFinishing) return
        try {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (_: Exception) {}
        finish()
    }

    private fun bind() {
        particle = findViewById(R.id.particleView); glow = findViewById(R.id.glowView)
        ring = findViewById(R.id.ringView); logo = findViewById(R.id.logoImage)
        tvBrand1 = findViewById(R.id.tvBrand1); tvBrand2 = findViewById(R.id.tvBrand2)
        tvSubtitle = findViewById(R.id.tvSubtitle); tvLoading = findViewById(R.id.tvLoading)
        tvVersion = findViewById(R.id.tvVersion); divider = findViewById(R.id.dividerView)
        progress = findViewById(R.id.progressBar)
        dot1 = findViewById(R.id.dot1); dot2 = findViewById(R.id.dot2); dot3 = findViewById(R.id.dot3)
    }

    private fun orchestrate() {
        post(300) { particle.animate().alpha(1f).setDuration(1200).start() }
        post(500) { glow.animate().alpha(1f).setDuration(1000).start(); startGlow() }
        post(800) { ring.alpha = 0f; ring.animate().alpha(0.45f).setDuration(800).start(); startRing() }
        post(1200) {
            logo.alpha = 0f; logo.scaleX = 0.3f; logo.scaleY = 0.3f
            logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(1200)
                .setInterpolator(OvershootInterpolator(2f)).start()
        }
        post(1800) { startBrand() }
        post(2800) {
            tvSubtitle.alpha = 0f; tvSubtitle.translationY = 30f
            tvSubtitle.animate().alpha(1f).translationY(0f).setDuration(900)
                .setInterpolator(DecelerateInterpolator()).start()
        }
        post(3200) {
            divider.scaleX = 0f; divider.alpha = 0f
            divider.animate().scaleX(1f).alpha(1f).setDuration(800)
                .setInterpolator(OvershootInterpolator(2f)).start()
        }
        post(3500) {
            val c = progress.parent as? View ?: return@post
            c.scaleX = 0f; c.scaleY = 0f; c.alpha = 0f
            c.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(900)
                .setInterpolator(OvershootInterpolator(1.6f)).start()
        }
        post(4000) {
            tvLoading.alpha = 0f; tvLoading.animate().alpha(1f).setDuration(800).start()
            dot1.alpha = 0f; dot2.alpha = 0f; dot3.alpha = 0f
            dot1.animate().alpha(1f).setDuration(400).start()
            dot2.animate().alpha(1f).setStartDelay(150).setDuration(400).start()
            dot3.animate().alpha(1f).setStartDelay(300).setDuration(400).start()
            startDots()
        }
        post(4500) { tvVersion.animate().alpha(1f).setDuration(1000).start(); startProgress() }
    }

    private fun post(delay: Long, action: () -> Unit) {
        handler.postDelayed({
            if (isFinishing) return@postDelayed
            try { action() } catch (_: Exception) {}
        }, delay)
    }

    private fun startGlow() {
        val a = ValueAnimator.ofFloat(0.35f, 0.75f, 0.35f)
        a.duration = 4000; a.repeatCount = ValueAnimator.INFINITE
        a.interpolator = AccelerateDecelerateInterpolator()
        a.addUpdateListener {
            val f = it.animatedValue as Float
            glow.alpha = f; val s = 0.9f + (f - 0.35f) * 0.6f
            glow.scaleX = s; glow.scaleY = s
        }
        a.start(); anims.add(a)
    }

    private fun startRing() {
        ring.animate().rotation(360f).setDuration(15000)
            .setInterpolator(LinearInterpolator())
            .withEndAction { if (!isFinishing) startRing() }.start()
    }

    private fun startBrand() {
        val t1 = "REMOTE "; val t2 = "CONSOLE"; val iv = 70L
        t1.forEachIndexed { i, _ ->
            handler.postDelayed({
                if (isFinishing) return@postDelayed
                tvBrand1.text = t1.substring(0, i + 1); tvBrand1.alpha = 1f
                tvBrand1.translationY = -8f
                tvBrand1.animate().translationY(0f).setDuration(200).start()
            }, i * iv)
        }
        val off = t1.length * iv + 100
        t2.forEachIndexed { i, _ ->
            handler.postDelayed({
                if (isFinishing) return@postDelayed
                tvBrand2.text = t2.substring(0, i + 1); tvBrand2.alpha = 1f
                tvBrand2.translationY = -8f
                tvBrand2.animate().translationY(0f).setDuration(200).start()
            }, off + i * iv)
        }
    }

    private fun startDots() {
        listOf(dot1, dot2, dot3).forEachIndexed { i, d ->
            val a = ObjectAnimator.ofFloat(d, "translationY", 0f, -16f, 0f)
            a.duration = 900; a.startDelay = i * 150L
            a.repeatCount = ObjectAnimator.INFINITE
            a.interpolator = AccelerateDecelerateInterpolator()
            a.start(); anims.add(a)
        }
    }

    private fun startProgress() {
        progress.post {
            progress.max = 1000
            val a = ObjectAnimator.ofInt(progress, "progress", 0, 1000)
            a.duration = 15_500L; a.interpolator = DecelerateInterpolator(1.2f)
            a.start(); anims.add(a)
            val stages = listOf(
                "正在初始化核心模块..." to Color.parseColor("#4A6BFF"),
                "正在加载局域网协议栈..." to Color.parseColor("#5A6BFF"),
                "正在建立加密通道..." to Color.parseColor("#7A5BFF"),
                "即将进入控制台..." to Color.parseColor("#E94560"))
            stages.forEachIndexed { i, (t, c) ->
                handler.postDelayed({
                    if (isFinishing) return@postDelayed
                    tvLoading.animate().alpha(0f).setDuration(200).withEndAction {
                        tvLoading.text = t; tvLoading.setTextColor(c)
                        tvLoading.animate().alpha(1f).setDuration(300).start()
                    }.start()
                }, i * 5000L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        anims.forEach { try { it.cancel() } catch (_: Exception) {} }
        anims.clear()
    }
}
