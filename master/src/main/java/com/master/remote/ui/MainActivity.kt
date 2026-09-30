package com.master.remote.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    private val TAG = "Main"
    private val devices = mutableListOf<DeviceInfo>()
    private var adapter: DeviceAdapter? = null
    private var discovery: MdnsDiscovery? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
        } catch (e: Throwable) {
            Log.e(TAG, "setContentView failed", e)
            finish()
            return
        }

        try {
            val rv = findViewById<RecyclerView>(R.id.rvDevices)
            rv.layoutManager = LinearLayoutManager(this)
            adapter = DeviceAdapter(devices) { d -> openControl(d) }
            rv.adapter = adapter
        } catch (e: Throwable) {
            Log.e(TAG, "recycler init failed", e)
        }

        try {
            findViewById<View>(R.id.btnScan).setOnClickListener {
                it.animate().rotationBy(360f).setDuration(600).start()
                startScan()
            }
        } catch (e: Throwable) { Log.e(TAG, "btnScan", e) }

        initDiscovery()
    }

    private fun initDiscovery() {
        try {
            discovery = MdnsDiscovery(
                this,
                onFound = { d ->
                    runOnUiThread {
                        try {
                            if (devices.none { it.ip == d.ip && it.port == d.port }) {
                                devices.add(d)
                                adapter?.notifyDataSetChanged()
                                updateCount()
                            }
                        } catch (_: Throwable) {}
                    }
                },
                onLost = { name ->
                    runOnUiThread {
                        try {
                            devices.removeAll { it.name == name }
                            adapter?.notifyDataSetChanged()
                            updateCount()
                        } catch (_: Throwable) {}
                    }
                }
            )
            startScan()
        } catch (e: Throwable) {
            Log.e(TAG, "initDiscovery", e)
        }
    }

    private fun startScan() {
        try { discovery?.start() } catch (_: Throwable) {}
    }

    private fun updateCount() {
        try {
            findViewById<TextView>(R.id.tvCount).text = devices.size.toString()
        } catch (_: Throwable) {}
    }

    private fun openControl(d: DeviceInfo) {
        try {
            val i = Intent(this, ControlActivity::class.java)
            i.putExtra("ip", d.ip)
            i.putExtra("port", d.port)
            i.putExtra("name", d.name)
            startActivity(i)
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try { discovery?.stop() } catch (_: Throwable) {}
    }
}
