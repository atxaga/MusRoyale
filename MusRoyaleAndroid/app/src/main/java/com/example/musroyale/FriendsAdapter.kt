package com.example.musroyale

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FriendsAdapter(
    private var items: MutableList<Map<String, String>>,
    private val modo: String // "BUSCAR", "RECIBIDAS", "ENVIADAS"
) : RecyclerView.Adapter<FriendsAdapter.VH>() {

    private var original = ArrayList(items)
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.friend_card)
        val layout: ConstraintLayout = view.findViewById(R.id.friend_layout)
        val name: TextView = view.findViewById(R.id.name)
        val statusText: TextView = view.findViewById(R.id.status_text) // <-- Nuevo
        val action: Button = view.findViewById(R.id.btn_action)
        val avatar: ImageView = view.findViewById(R.id.avatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = items[position]
        val userId = user["id"].toString() // Recuperamos el ID
        val relacion = user["relacion"] ?: "EXPLORAR" // Recuperamos la relación
        val esPremium = user["premium"] == "true"
        holder.name.text = user["username"] ?: "Sin nombre"

        val estadoRef = com.google.firebase.database.FirebaseDatabase
            .getInstance("https://musroyale-488aa-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("estado_usuarios")
            .child(userId)

        estadoRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val estado = snapshot.getValue(String::class.java) ?: "offline"
                if (estado == "online") {
                    holder.statusText.text = "● Online"
                    holder.statusText.setTextColor(Color.parseColor("#2ECC71"))
                } else {
                    holder.statusText.text = "○ Offline"
                    holder.statusText.setTextColor(Color.GRAY)
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
        // 1. RESETEO POR DEFECTO (Importante para el reciclaje de celdas)
        holder.layout.setBackgroundResource(R.drawable.bg_gold_card_gradient)
        holder.card.strokeColor = Color.parseColor("#E0C9A6")
        holder.card.strokeWidth = 1
        holder.name.setTextColor(Color.parseColor("#5D4037"))
        holder.name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0) // Quitar estrella
        holder.action.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#BFA06A")))
        holder.action.setTextColor(Color.WHITE)

        val avatarImg = holder.avatar as com.google.android.material.imageview.ShapeableImageView
        avatarImg.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#BFA06A"))

        // 2. APLICAR ESTILO PREMIUM
        if (esPremium) {
            // Usamos el fondo azul que mencionaste (asegúrate de que el nombre sea correcto)
            holder.layout.setBackgroundResource(R.drawable.bg_subscription_gold)

            holder.card.strokeColor = Color.parseColor("#FFD700")
            holder.card.strokeWidth = 3

            holder.name.setTextColor(Color.WHITE)
            holder.name.compoundDrawablePadding = 8
            holder.name.compoundDrawables[2]?.setTint(Color.parseColor("#FFD700"))

            holder.action.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFD700")))
            holder.action.setTextColor(Color.BLACK)

            avatarImg.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFD700"))
        }

        // --- LÓGICA PARA EL AVATAR ---
        val avatarNombre = user["avatarActual"] ?: "avadef.png"
        val context = holder.itemView.context
        val cleanName = avatarNombre.replace(".png", "")
        val resId = context.resources.getIdentifier(cleanName, "drawable", context.packageName)

        if (resId != 0) holder.avatar.setImageResource(resId)
        else holder.avatar.setImageResource(R.drawable.ic_avatar3)

        // --- CONFIGURACIÓN DE BOTONES (Usando la variable 'relacion') ---
        when (modo) {
            "RECIBIDAS" -> {
                holder.action.text = "Onartu"
                holder.action.setOnClickListener { aceptarSolicitud(userId, context, holder.adapterPosition) }
            }
            "ENVIADAS" -> {
                holder.action.text = "Kendu"
                holder.action.setOnClickListener {
                    cancelarSolicitudEnviada(userId, context)
                    items.removeAt(holder.adapterPosition)
                    notifyItemRemoved(holder.adapterPosition)
                }
            }
            "BUSCAR" -> {
                when (relacion) {
                    "AMIGO" -> {
                        holder.action.text = "Laguna"
                        holder.action.isEnabled = false
                        holder.action.alpha = 0.5f
                    }
                    "PENDIENTE" -> {
                        holder.action.text = "Bidalita"
                        holder.action.isEnabled = false
                        holder.action.alpha = 0.5f
                    }
                    else -> {
                        holder.action.text = "Gehitu"
                        holder.action.isEnabled = true
                        holder.action.alpha = 1.0f
                        holder.action.setOnClickListener {
                            enviarSolicitudAmistad(userId, context)
                            holder.action.text = "Bidalita"
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
            // REGLA: Si el buscador está vacío, mostrar SOLO los que ya son amigos
            val soloAmigos = original.filter { it["relacion"] == "AMIGO" }
            items.addAll(soloAmigos)
        } else {
            // REGLA: Si el usuario está buscando, buscar en la lista ORIGINAL (todos los usuarios)
            val filteredList = original.filter {
                val username = it["username"]?.lowercase() ?: ""
                username.contains(lowerCaseQuery)
            }
            items.addAll(filteredList)
        }
        notifyDataSetChanged()
    }
}