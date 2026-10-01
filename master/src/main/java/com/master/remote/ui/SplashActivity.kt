package com.master.remote.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.master.remote.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_splash) } catch (_: Throwable) {}
        try {
            val logo = findViewById(R.id.tvLogo) as? TextView
            val ring = findViewById(R.id.ringView) as? View
            if (logo != null) {
                logo.scaleX = 0.3f; logo.scaleY = 0.3f; logo.alpha = 0f
                logo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(900).start()
            }
            ring?.animate()?.rotation(360f)?.setDuration(3000)?.start()
        } catch (_: Throwable) {}
        Handler(Looper.getMainLooper()).postDelayed({
            try { startActivity(Intent(this, MainActivity::class.java)) } catch (_: Throwable) {}
            finish()
        }, 2500L)
    }
}
