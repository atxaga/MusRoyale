package com.example.musroyale

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FriendsAdapter(
    private var items: MutableList<Map<String, String>>,
    private val modo: String // "BUSCAR", "RECIBIDAS", "ENVIADAS"
) : RecyclerView.Adapter<FriendsAdapter.VH>() {

    private var original = ArrayList(items)
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val action: Button = view.findViewById(R.id.btn_action)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = items[position]

        val userId = user["id"].toString()
        val relacion = user["relacion"] ?: "EXPLORAR"
        holder.name.text = user["username"]
        // Configuramos el botón según el modo de la pantalla
        when (modo) {
            "RECIBIDAS" -> {
                holder.action.text = "Aceptar"
                holder.action.isEnabled = true
                holder.action.alpha = 1.0f
                holder.action.setOnClickListener {
                    // CORRECCIÓN: Ahora pasamos holder.adapterPosition como tercer parámetro
                    aceptarSolicitud(userId, holder.itemView.context, holder.adapterPosition)
                }
            }
            "ENVIADAS" -> {
                holder.action.text = "Cancelar"
                holder.action.isEnabled = true
                holder.action.alpha = 1.0f
                holder.action.setOnClickListener {
                    cancelarSolicitudEnviada(userId, holder.itemView.context)
                    // Eliminamos el item de la lista visualmente
                    items.removeAt(holder.adapterPosition)
                    notifyItemRemoved(holder.adapterPosition)
                }
            }
            "BUSCAR" -> {
                when (relacion) {
                    "AMIGO" -> {
                        holder.action.text = "Amigo"
                        holder.action.isEnabled = false
                        holder.action.alpha = 0.5f
                    }
                    "PENDIENTE" -> {
                        holder.action.text = "Enviada"
                        holder.action.isEnabled = false
                        holder.action.alpha = 0.5f
                    }
                    else -> { // EXPLORAR
                        holder.action.text = "Solicitar"
                        holder.action.isEnabled = true
                        holder.action.alpha = 1.0f
                        holder.action.setOnClickListener {
                            enviarSolicitudAmistad(userId, holder.itemView.context)
                            holder.action.text = "Enviada"
                            holder.action.isEnabled = false
                            holder.action.alpha = 0.5f
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // --- TUS FUNCIONES DE FIREBASE ---

    fun enviarSolicitudAmistad(targetUserId: String, context: Context) {
        val prefs = context.getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentUserEmail = prefs.getString("userRegistrado", null) ?: return
        val batch = db.batch()
        val currentUserRef = db.collection("Users").document(currentUserEmail)
        val targetUserRef = db.collection("Users").document(targetUserId)

        batch.update(currentUserRef, "solicitudMandada", FieldValue.arrayUnion(targetUserId))
        batch.update(targetUserRef, "solicitudRecivida", FieldValue.arrayUnion(currentUserEmail))
        batch.commit()
    }

    // Añadimos ', position: Int' para que acepte el tercer argumento
    fun aceptarSolicitud(targetUserId: String, context: Context, position: Int) {
        val prefs = context.getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentUserEmail = prefs.getString("userRegistrado", null) ?: return

        val batch = db.batch()
        val meRef = db.collection("Users").document(currentUserEmail)
        val friendRef = db.collection("Users").document(targetUserId)

        batch.update(meRef, "solicitudRecivida", FieldValue.arrayRemove(targetUserId))
        batch.update(meRef, "amigos", FieldValue.arrayUnion(targetUserId))
        batch.update(friendRef, "solicitudMandada", FieldValue.arrayRemove(currentUserEmail))
        batch.update(friendRef, "amigos", FieldValue.arrayUnion(currentUserEmail))

        batch.commit().addOnSuccessListener {
            // Esto elimina el registro de la pantalla en tiempo real
            if (position != RecyclerView.NO_POSITION && position < items.size) {
                items.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    }

    private fun cancelarSolicitudEnviada(targetUserId: String, context: Context) {
        val prefs = context.getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val myEmail = prefs.getString("userRegistrado", null) ?: return
        val batch = db.batch()
        val meRef = db.collection("Users").document(myEmail)
        val targetRef = db.collection("Users").document(targetUserId)

        batch.update(meRef, "solicitudMandada", FieldValue.arrayRemove(targetUserId))
        batch.update(targetRef, "solicitudRecivida", FieldValue.arrayRemove(myEmail))
        batch.commit()
    }

    fun updateData(newList: List<Map<String, String>>) {
        items.clear()
        items.addAll(newList)
        original = ArrayList(items)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val lowerCaseQuery = query.lowercase().trim()
        items.clear()

        if (lowerCaseQuery.isEmpty()) {
            // Si no hay búsqueda, volvemos a mostrar la lista original (que ya está ordenada)
            items.addAll(original)
        } else {
            // Filtramos por el nombre de usuario
            val filteredList = original.filter {
                val username = it["username"]?.lowercase() ?: ""
                username.contains(lowerCaseQuery)
            }
            items.addAll(filteredList)
        }
        notifyDataSetChanged()
    }
}