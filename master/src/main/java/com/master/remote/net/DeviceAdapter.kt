package com.master.remote.net

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.master.remote.R

class DeviceAdapter(
    private val items: List<DeviceInfo>,
    private val onClick: (DeviceInfo) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvName)
        val info: TextView = v.findViewById(R.id.tvInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        try {
            val d = items[position]
            holder.name.text = d.name
            holder.info.text = "${d.ip}:${d.port}"
            holder.itemView.setOnClickListener { onClick(d) }
        } catch (_: Throwable) {}
    }

    override fun getItemCount() = items.size
}
