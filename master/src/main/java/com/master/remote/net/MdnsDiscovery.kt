package com.master.remote.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class MdnsDiscovery(
    private val context: Context,
    private val onFound: (DeviceInfo) -> Unit,
    private val onLost: (String) -> Unit
) {
    private val tag = "Mdns"
    private var nsdManager: NsdManager? = null
    private var listener: NsdManager.DiscoveryListener? = null
    private var running = false

    fun start() {
        if (running) return
        running = true
        try {
            nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
            listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String?) { Log.i(tag, "start $serviceType") }
                override fun onServiceFound(info: NsdServiceInfo) {
                    if (info.serviceType?.contains("remoteconsole") != true) return
                    nsdManager?.resolveService(info, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(si: NsdServiceInfo?, e: Int) { Log.w(tag, "fail $e") }
                        override fun onServiceResolved(si: NsdServiceInfo?) {
                            si ?: return
                            val host = si.host?.hostAddress ?: return
                            onFound(DeviceInfo(si.serviceName ?: "Device", host, si.port))
                        }
                    })
                }
                override fun onServiceLost(info: NsdServiceInfo?) { onLost(info?.serviceName ?: "") }
                override fun onDiscoveryStopped(serviceType: String?) {}
                override fun onStartDiscoveryFailed(t: String?, e: Int) { Log.e(tag, "startFail $e") }
                override fun onStopDiscoveryFailed(t: String?, e: Int) { Log.e(tag, "stopFail $e") }
            }
            nsdManager?.discoverServices("_remoteconsole._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) { Log.e(tag, "start", e) }
    }

    fun stop() {
        if (!running) return
        running = false
        try { nsdManager?.stopServiceDiscovery(listener) } catch (_: Exception) {}
    }
}
