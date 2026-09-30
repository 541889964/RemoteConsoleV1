package com.master.remote.ui

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.master.remote.R
import com.master.remote.net.DeviceInfo
import com.master.remote.net.MdnsDiscovery

class MainActivity : AppCompatActivity() {

    private val devices = mutableListOf<DeviceInfo>()
    private lateinit var adapter: DeviceAdapter
    private var discovery: MdnsDiscovery? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_main) } catch (_: Exception) { finish(); return }

        safe {
            findViewById<View>(R.id.statCard)?.apply {
                alpha = 0f; translationY = 40f
                animate().alpha(1f).translationY(0f).setDuration(700).setStartDelay(100)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
            }
            findViewById<View>(R.id.bottomBar)?.apply {
                alpha = 0f; translationY = 80f
                animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(300)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
            }
        }
        safe {
            findViewById<FrameLayout>(R.id.btnScan)?.setOnClickListener {
                it.animate().rotationBy(360f).setDuration(800)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
                startScan()
            }
        }
        safe { findViewById<View>(R.id.pulseRing1)?.let { startPulse(it, 0) } }
        safe { findViewById<View>(R.id.pulseRing2)?.let { startPulse(it, 1200) } }
        safe {
            val rv = findViewById<RecyclerView>(R.id.rvDevices)
            rv.layoutManager = LinearLayoutManager(this)
            adapter = DeviceAdapter(devices) { d -> openControl(d) }
            rv.adapter = adapter
        }
        initDiscovery()
    }

    private fun initDiscovery() {
        discovery = MdnsDiscovery(this,
            onFound = { d ->
                runOnUiThread {
                    if (devices.none { it.ip == d.ip && it.port == d.port }) {
                        devices.add(d); adapter.notifyDataSetChanged(); updateCount()
                    }
                }
            },
            onLost = { name ->
                runOnUiThread {
                    devices.removeAll { it.name == name }
                    adapter.notifyDataSetChanged(); updateCount()
                }
            })
        startScan()
    }

    private fun startScan() { try { discovery?.start() } catch (_: Exception) {} }

    private fun updateCount() { safe { findViewById<TextView>(R.id.tvDeviceCount)?.text = devices.size.toString() } }

    private inline fun safe(block: () -> Unit) { try { block() } catch (_: Exception) {} }

    private fun startPulse(v: View, delay: Long) {
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 2400; a.startDelay = delay
        a.repeatCount = ValueAnimator.INFINITE
        a.interpolator = AccelerateDecelerateInterpolator()
        a.addUpdateListener {
            val f = it.animatedFraction
            v.scaleX = 0.9f + f * 0.5f
            v.scaleY = 0.9f + f * 0.5f
            v.alpha = (1f - f) * 0.5f
        }
        a.start()
    }

    private fun openControl(d: DeviceInfo) {
        try {
            val i = Intent(this, ControlActivity::class.java)
            i.putExtra("ip", d.ip); i.putExtra("port", d.port); i.putExtra("name", d.name)
            startActivity(i)
        } catch (_: Exception) {}
    }

    override fun onDestroy() { super.onDestroy(); try { discovery?.stop() } catch (_: Exception) {} }
}
