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
    private var nsd: NsdManager? = null
    private var listener: NsdManager.DiscoveryListener? = null
    private var running = false

    fun start() {
        if (running) return
        running = true
        try {
            nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
            listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(t: String?) {}
                override fun onServiceFound(info: NsdServiceInfo) {
                    if (info.serviceType?.contains("remoteconsole") != true) return
                    try {
                        nsd?.resolveService(info, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(si: NsdServiceInfo?, e: Int) {}
                            override fun onServiceResolved(si: NsdServiceInfo?) {
                                si ?: return
                                val host = si.host?.hostAddress ?: return
                                try { onFound(DeviceInfo(si.serviceName ?: "Device", host, si.port)) } catch (_: Throwable) {}
                            }
                        })
                    } catch (_: Throwable) {}
                }
                override fun onServiceLost(info: NsdServiceInfo?) { try { onLost(info?.serviceName ?: "") } catch (_: Throwable) {} }
                override fun onDiscoveryStopped(t: String?) {}
                override fun onStartDiscoveryFailed(t: String?, e: Int) { Log.e(tag, "start $e") }
                override fun onStopDiscoveryFailed(t: String?, e: Int) {}
            }
            nsd?.discoverServices("_remoteconsole._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Throwable) { Log.e(tag, "start", e) }
    }

    fun stop() {
        if (!running) return
        running = false
        try { nsd?.stopServiceDiscovery(listener) } catch (_: Throwable) {}
    }
}
