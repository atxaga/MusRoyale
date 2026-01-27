package com.example.musroyale

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class HistorialAdapter(
    private val solicitudes: List<SolicitudPago>,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<HistorialAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtEstado: TextView = view.findViewById(R.id.txtEstadoHistorial)
        val txtMonto: TextView = view.findViewById(R.id.txtMontoHistorial)
        val txtId: TextView = view.findViewById(R.id.txtIdHistorial)
        val txtTipo: TextView = view.findViewById(R.id.txtTipoOperacion)
        val btnDelete: Button = view.findViewById(R.id.btnEliminarHistorial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial_pago, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val s = solicitudes[position]

        // 1. Configurar textos según el tipo de operación
        // En tu lógica: "retirada" es sacar dinero, "pendiente" (o cualquier otro) es inyección
        if (s.status == "retirada") {
            holder.txtTipo.text = "ERRETIROA"
            holder.txtTipo.setTextColor(Color.parseColor("#FF5252")) // Rojo
            holder.txtMonto.text = String.format(Locale.US, "-%.2f €", s.monto)
            holder.txtMonto.setTextColor(Color.parseColor("#FF5252"))
        } else {
            holder.txtTipo.text = "INJEKZIOA"
            holder.txtTipo.setTextColor(Color.parseColor("#00E676")) // Verde
            holder.txtMonto.text = String.format(Locale.US, "+%.2f €", s.monto)
            holder.txtMonto.setTextColor(Color.parseColor("#00E676"))
        }

        // 2. Mostrar ID de referencia
        holder.txtId.text = "ID: ${s.orderId}"

        // 3. Gestionar el texto y color del Estado
        when (s.status) {
            "aprobado" -> {
                holder.txtEstado.text = "ONARTUA"
                holder.txtEstado.setTextColor(Color.GREEN)
            }
            "rechazado" -> {
                holder.txtEstado.text = "EZEZTATUA"
                holder.txtEstado.setTextColor(Color.RED)
            }
            else -> {
                holder.txtEstado.text = "PENDIENTE"
                holder.txtEstado.setTextColor(Color.parseColor("#FFD700")) // Dorado
            }
        }

        // 4. Botón de eliminar
        holder.btnDelete.setOnClickListener {
            onDelete(s.idDoc)
        }
    }

    override fun getItemCount(): Int = solicitudes.size
}