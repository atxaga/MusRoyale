package com.example.musroyale

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminPagosAdapter(
    private val solicitudes: List<SolicitudPago>,
    private val onAprobarClick: (SolicitudPago) -> Unit
) : RecyclerView.Adapter<AdminPagosAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtInfo: TextView = view.findViewById(R.id.txtInfoSolicitud)
        val btnAprobar: Button = view.findViewById(R.id.btnAprobar)
        val txtMontoAdmin: TextView = view.findViewById(R.id.txtMontoAdmin)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_solicitud_pago, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = solicitudes[position]
        holder.txtMontoAdmin.text = "${item.monto} €"
        holder.txtInfo.text = "${item.username} - ID: ${item.orderId}"
        holder.btnAprobar.setOnClickListener { onAprobarClick(item) }
    }

    override fun getItemCount() = solicitudes.size
}