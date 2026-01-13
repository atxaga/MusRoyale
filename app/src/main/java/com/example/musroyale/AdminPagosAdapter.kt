package com.example.musroyale

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminPagosAdapter(
    private val solicitudes: List<SolicitudPago>,
    private val onAprobarClick: (SolicitudPago) -> Unit,
    private val onRechazarClick: (SolicitudPago) -> Unit // <--- Añadido el parámetro faltante
) : RecyclerView.Adapter<AdminPagosAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtInfo: TextView = view.findViewById(R.id.txtInfoSolicitud)
        val btnAprobar: Button = view.findViewById(R.id.btnAprobar)
        val txtMontoAdmin: TextView = view.findViewById(R.id.txtMontoAdmin)
        val btnRechazar: Button = view.findViewById(R.id.btnRechazar) // <--- Asegúrate que este ID existe en tu XML
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_solicitud_pago, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = solicitudes[position]

        // Configuración visual según si es Retirada o Ingreso
        if (item.status == "retirada") {
            holder.txtMontoAdmin.text = "- ${String.format("%.2f", item.monto)} €"
            holder.txtMontoAdmin.setTextColor(Color.parseColor("#FF5252")) // Rojo
            holder.btnAprobar.text = "ERRETIRATU"
        } else {
            holder.txtMontoAdmin.text = "+ ${String.format("%.2f", item.monto)} €"
            holder.txtMontoAdmin.setTextColor(Color.parseColor("#00E676")) // Verde
            holder.btnAprobar.text = "ONARTU"
        }

        holder.txtInfo.text = item.username

        // Asignación de clics
        holder.btnAprobar.setOnClickListener { onAprobarClick(item) }
        holder.btnRechazar.setOnClickListener { onRechazarClick(item) }
    }

    override fun getItemCount() = solicitudes.size
}