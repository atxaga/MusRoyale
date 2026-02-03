package com.example.musroyale

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musroyale.databinding.ActivityDuosBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase

class DuoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDuosBinding
    private val db = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance("https://musroyale-488aa-default-rtdb.europe-west1.firebasedatabase.app/")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDuosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lógica de los botones de apuesta (±5)
        setupApuestaButtons()

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.btnInviteFriend.setOnClickListener {
            showInviteDialog()
        }

        binding.btnPlay.setOnClickListener {
            val apuesta = binding.etApuestaValue.text.toString()
            Toast.makeText(this, "Partida hasten: $apuesta fitxa", Toast.LENGTH_SHORT).show()
            // Aquí iría la lógica para crear la partida en Firebase
        }
    }

    private fun setupApuestaButtons() {
        binding.btnMinus.setOnClickListener {
            val actual = binding.etApuestaValue.text.toString().toIntOrNull() ?: 0
            if (actual >= 5) binding.etApuestaValue.setText((actual - 5).toString())
        }

        binding.btnPlus.setOnClickListener {
            val actual = binding.etApuestaValue.text.toString().toIntOrNull() ?: 0
            binding.etApuestaValue.setText((actual + 5).toString())
        }
    }

    private fun showInviteDialog() {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val myEmail = prefs.getString("userRegistrado", null) ?: return

        // 1. Obtener mi lista de IDs de amigos
        db.collection("Users").document(myEmail).get().addOnSuccessListener { doc ->
            val amigosIds = doc.get("amigos") as? List<String> ?: listOf()

            if (amigosIds.isEmpty()) {
                Toast.makeText(this, "Ez duzu lagunik oraindik", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            // 2. Obtener los datos (username, avatar) de esos amigos
            db.collection("Users").whereIn(com.google.firebase.firestore.FieldPath.documentId(), amigosIds)
                .get().addOnSuccessListener { query ->
                    val listaAmigos = query.documents.map { d ->
                        mutableMapOf(
                            "id" to d.id,
                            "username" to (d.getString("username") ?: "Sin nombre"),
                            "avatarActual" to (d.getString("avatarActual") ?: "avadef.png")
                        )
                    }
                    mostrarDialogoCustom(listaAmigos)
                }
        }
    }

    private fun mostrarDialogoCustom(lista: List<MutableMap<String, String>>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_friends_list, null)
        val rv = dialogView.findViewById<RecyclerView>(R.id.rvInviteFriends)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelDialog)

        rv.layoutManager = LinearLayoutManager(this)

        // Crear el diálogo
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setBackground(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)) // Quita el fondo blanco por defecto
            .create()

        rv.adapter = InviteAdapter(lista) { userId ->
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()

        // Ajustar el tamaño del diálogo para que no pegue a los bordes de la pantalla
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(), // 90% del ancho
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Adaptador Interno para el diálogo de invitación
    inner class InviteAdapter(
        private val friends: List<Map<String, String>>,
        private val onInvite: (String) -> Unit
    ) : RecyclerView.Adapter<InviteAdapter.InviteVH>() {

        inner class InviteVH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.nameInvite)
            val status: TextView = v.findViewById(R.id.statusInvite)
            val avatar: ImageView = v.findViewById(R.id.avatarInvite)
            val btn: Button = v.findViewById(R.id.btnInviteAction)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteVH {
            // USAMOS EL NUEVO LAYOUT
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_friend_invite, parent, false)
            return InviteVH(v)
        }

        override fun onBindViewHolder(holder: InviteVH, position: Int) {
            val user = friends[position]
            val userId = user["id"] ?: ""
            holder.name.text = user["username"]

            // Manejo de Avatar
            val avatarNombre = user["avatarActual"] ?: "avadef"
            val cleanName = avatarNombre.replace(".png", "")
            val resId = resources.getIdentifier(cleanName, "drawable", packageName)
            holder.avatar.setImageResource(if (resId != 0) resId else R.drawable.ic_avatar3)

            // Estado en tiempo real
            realtimeDb.getReference("estado_usuarios").child(userId)
                .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val estado = snapshot.getValue(String::class.java) ?: "offline"
                        if (estado == "online") {
                            holder.status.text = "● Online"
                            holder.status.setTextColor(Color.parseColor("#2ECC71"))
                        } else {
                            holder.status.text = "○ Offline"
                            holder.status.setTextColor(Color.GRAY)
                        }
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                })

            holder.btn.setOnClickListener { onInvite(userId) }
        }

        override fun getItemCount() = friends.size
    }
}