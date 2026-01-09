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
        // Actualizamos los IDs para que coincidan con el diseño elegante
        val txtUsername: TextView = view.findViewById(R.id.txtUsernameAdmin)
        val txtMonto: TextView = view.findViewById(R.id.txtMontoAdmin)
        val txtOrderId: TextView = view.findViewById(R.id.txtOrderIdAdmin)
        val btnAprobar: Button = view.findViewById(R.id.btnAprobar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_solicitud_pago, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = solicitudes[position]

        // Asignamos los datos a cada campo por separado
        holder.txtUsername.text = item.username
        holder.txtMonto.text = "${item.monto} USDT"
        holder.txtOrderId.text = item.orderId

        holder.btnAprobar.setOnClickListener {
            onAprobarClick(item)
        }
    }

    override fun getItemCount() = solicitudes.size
}