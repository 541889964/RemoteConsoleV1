package com.master.remote.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

class MdnsDiscovery(
    private val context: Context,
    private val onFound: (DeviceInfo) -> Unit,
    private val onLost: (String) -> Unit
) {
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
                    try {
                        nsd?.resolveService(info, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(si: NsdServiceInfo?, e: Int) {}
                            override fun onServiceResolved(si: NsdServiceInfo?) {
                                if (si == null) return
                                val host = si.host?.hostAddress ?: return
                                onFound(DeviceInfo(si.serviceName ?: "Device", host, si.port))
                            }
                        })
                    } catch (_: Throwable) {}
                }
                override fun onServiceLost(info: NsdServiceInfo?) { onLost(info?.serviceName ?: "") }
                override fun onDiscoveryStopped(t: String?) {}
                override fun onStartDiscoveryFailed(t: String?, e: Int) {}
                override fun onStopDiscoveryFailed(t: String?, e: Int) {}
            }
            nsd?.discoverServices("_remoteconsole._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (_: Throwable) {}
    }

    fun stop() {
        if (!running) return
        running = false
        try { nsd?.stopServiceDiscovery(listener) } catch (_: Throwable) {}
    }
}
