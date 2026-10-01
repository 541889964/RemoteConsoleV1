package com.master.remote.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.master.remote.R
import com.master.remote.net.DeviceAdapter
import com.master.remote.net.DeviceInfo
import com.master.remote.net.MdnsDiscovery

class MainActivity : AppCompatActivity() {
    private val devices = ArrayList<DeviceInfo>()
    private var adapter: DeviceAdapter? = null
    private var discovery: MdnsDiscovery? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { setContentView(R.layout.activity_main) } catch (_: Throwable) { finish(); return }

        try {
            val rv = findViewById(R.id.rvDevices) as? RecyclerView
            rv?.layoutManager = LinearLayoutManager(this)
            adapter = DeviceAdapter(devices) { d -> openControl(d) }
            rv?.adapter = adapter
        } catch (_: Throwable) {}

        try {
            val btn = findViewById(R.id.btnScan) as? View
            btn?.setOnClickListener {
                it.animate().rotationBy(360f).setDuration(600).start()
                setStatus("扫描中...")
                startScan()
            }
        } catch (_: Throwable) {}

        initDiscovery()
        updateCount()
    }

    private fun initDiscovery() {
        try {
            discovery = MdnsDiscovery(this,
                { d ->
                    runOnUiThread {
                        var exists = false
                        for (i in devices) if (i.ip == d.ip && i.port == d.port) exists = true
                        if (!exists) {
                            devices.add(d)
                            adapter?.notifyDataSetChanged()
                            updateCount()
                        }
                    }
                },
                { name ->
                    runOnUiThread {
                        val toRemove = ArrayList<DeviceInfo>()
                        for (i in devices) if (i.name == name) toRemove.add(i)
                        devices.removeAll(toRemove)
                        adapter?.notifyDataSetChanged()
                        updateCount()
                    }
                })
            startScan()
        } catch (_: Throwable) {}
    }

    private fun startScan() { try { discovery?.start() } catch (_: Throwable) {} }

    private fun updateCount() {
        try { (findViewById(R.id.tvCount) as? TextView)?.text = devices.size.toString() } catch (_: Throwable) {}
        setStatus("${devices.size} 台设备在线")
    }

    private fun setStatus(s: String) {
        try { (findViewById(R.id.tvStatus) as? TextView)?.text = s } catch (_: Throwable) {}
    }

    private fun openControl(d: DeviceInfo) {
        try {
            val i = Intent(this, ControlActivity::class.java)
            i.putExtra("ip", d.ip); i.putExtra("port", d.port); i.putExtra("name", d.name)
            startActivity(i)
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try { discovery?.stop() } catch (_: Throwable) {}
    }
}
