package com.example.musroyale

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendsAdapter(private val items: MutableList<String>) : RecyclerView.Adapter<FriendsAdapter.VH>() {

    private val original = items.toMutableList()

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val action: Button = view.findViewById(R.id.btn_action)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val name = items[position]
        holder.name.text = name
        holder.action.setOnClickListener {
            // Acción de ejemplo: cambiar texto del botón
            holder.action.text = holder.action.context.getString(R.string.enviado)
        }
    }

    override fun getItemCount(): Int = items.size

    fun filter(query: String) {
        items.clear()
        if (query.isBlank()) {
            items.addAll(original)
        } else {
            val lower = query.lowercase()
            items.addAll(original.filter { it.lowercase().contains(lower) })
        }
        notifyDataSetChanged()
    }
}