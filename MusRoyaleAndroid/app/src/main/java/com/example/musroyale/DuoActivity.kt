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

class DuoActivity : BaseActivity() {

    private lateinit var binding: ActivityDuosBinding
    private lateinit var currentUser: String
    private val realtimeDb =
        FirebaseDatabase.getInstance("https://musroyale-488aa-default-rtdb.europe-west1.firebasedatabase.app/")
    private var idPartidaRecibida: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDuosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        idPartidaRecibida = intent.getStringExtra("idPartida")

        if (idPartidaRecibida != null) {
            escucharComoReceptor(idPartidaRecibida!!)
        } else {
            setupVistasNuevoEmisor()
        }
        var prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        currentUser = prefs.getString("userRegistrado", null) ?: ""

        binding.erregekopuruaLabel.visibility = View.VISIBLE
        binding.toggleGroupReyes.visibility = View.VISIBLE
        binding.apostuaLabel.visibility = View.VISIBLE
        binding.btnMinus.visibility = View.VISIBLE
        binding.btnPlus.visibility = View.VISIBLE
        binding.etApuestaValue.visibility = View.VISIBLE

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
        val uid = currentUser.ifEmpty {
            getSharedPreferences(
                "UserPrefs",
                MODE_PRIVATE
            ).getString("userRegistrado", "") ?: ""
        }
        if (uid.isEmpty()) return

        db.collection("Users").document(uid).get().addOnSuccessListener { snapshot ->
            val idsAmigos = snapshot.get("amigos") as? List<String> ?: emptyList()
            if (idsAmigos.isEmpty()) {
                Toast.makeText(this, "Ez duzu lagunik oraindik", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            db.collection("Users")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), idsAmigos).get()
                .addOnSuccessListener { query ->
                    val listaTemp = query.documents.map { d ->
                        mapOf(
                            "id" to d.id,
                            "username" to (d.getString("username") ?: "Sin nombre"),
                            "avatarActual" to (d.getString("avatarActual") ?: "avadef")
                        )
                    }

                    realtimeDb.getReference("estado_usuarios").get()
                        .addOnSuccessListener { stateSnapshot ->
                            val online = mutableListOf<Map<String, String>>()
                            val offline = mutableListOf<Map<String, String>>()

                            for (amigo in listaTemp) {
                                val status =
                                    stateSnapshot.child(amigo["id"]!!).getValue(String::class.java)
                                        ?: "offline"
                                if (status == "online") online.add(amigo) else offline.add(amigo)
                            }

                            val listaOrdenada = online + offline

                            val dialogView =
                                layoutInflater.inflate(R.layout.dialog_friends_list, null)
                            val rv = dialogView.findViewById<RecyclerView>(R.id.rvInviteFriends)
                            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelDialog)
                            rv.layoutManager = LinearLayoutManager(this)

                            val dialog = MaterialAlertDialogBuilder(this)
                                .setView(dialogView)
                                .setBackground(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                                .create()

                            // Pasamos el diálogo al adapter para que pueda cerrarlo al invitar
                            rv.adapter = InviteAdapter(listaOrdenada) {
                                dialog.dismiss()
                            }

                            btnCancel.setOnClickListener { dialog.dismiss() }
                            dialog.show()
                        }
                }
        }
    }


    private var esemisor = false
    private var esreceptor = false
    private var idPartidaActiva: String? = null


    private fun escucharComoReceptor(idPartida: String) {
        db.collection("PartidaDuo").document(idPartida)
            .addSnapshotListener { doc, e ->
                if (e != null || doc == null || !doc.exists()) {
                    finish()
                    return@addSnapshotListener
                }

                binding.btnPlay.visibility = View.GONE
                binding.btnInviteFriend.visibility = View.GONE
                binding.layoutGuestProfile.visibility = View.VISIBLE

                // Ocultar selectores de apuestas/reyes si los tienes
                binding.toggleGroupReyes?.visibility = View.GONE
                binding.btnPlus?.visibility = View.GONE
                binding.btnMinus?.visibility = View.GONE
                binding.apostuwhite.visibility = View.GONE
                binding.erregekopuruaLabel.visibility = View.GONE
                binding.apostuaLabel.visibility = View.GONE

                val onartua = doc.getBoolean("onartua") ?: false
                val jokatu = doc.getBoolean("jokatu") ?: false

                if (onartua) {
                    val emisorId = doc.getString("idemisor") ?: ""

                    db.collection("Users").document(emisorId).get().addOnSuccessListener { u ->
                        binding.tvGuestName.text = u.getString("username") ?: "Laguna"
                        val avatar = u.getString("avatarActual") ?: "avadef"
                        binding.ivGuestAvatar.setImageResource(
                            resources.getIdentifier(
                                avatar.replace(".png", ""),
                                "drawable",
                                packageName
                            )
                        )
                        binding.itxaroten.text = "Emisorak partida hastea itxaroten..."
                    }

                    if (jokatu) {
                        irAPartida(idPartida)
                    }
                }
            }
    }

    private fun escucharComoEmisor(idPartida: String) {
        db.collection("PartidaDuo").document(idPartida)
            .addSnapshotListener { doc, e ->
                if (e != null) return@addSnapshotListener

                if (doc == null || !doc.exists()) {
                    setupVistasNuevoEmisor()
                    return@addSnapshotListener
                }

                val onartua = doc.getBoolean("onartua") ?: false

                if (onartua) {
                    binding.btnInviteFriend.visibility = View.GONE
                    binding.layoutGuestProfile.visibility = View.VISIBLE

                    val idReceptor = doc.getString("idreceptor") ?: ""
                    db.collection("Users").document(idReceptor).get().addOnSuccessListener { u ->
                        binding.tvGuestName.text = u.getString("username") ?: "Laguna"
                        binding.itxaroten.text = "Laguna prest dago jolasteko!"
                        val avatar = u.getString("avatarActual") ?: "avadef"
                        binding.ivGuestAvatar.setImageResource(getResIdFromName(this, avatar))
                    }

                    binding.btnPlay.visibility = View.VISIBLE
                    binding.btnPlay.isEnabled = true
                    binding.btnPlay.alpha = 1.0f
                    binding.btnPlay.setOnClickListener {
                        doc.reference.update("jokatu", true)
                            .addOnSuccessListener { irAPartida(idPartida) }
                    }
                } else {
                    binding.btnInviteFriend.visibility = View.GONE
                    binding.layoutGuestProfile.visibility = View.VISIBLE
                    binding.tvGuestName.text = "Itxaroten..."
                    binding.itxaroten.text = "Gonbidapena bidalita..." // Aquí se queda fijo
                    binding.btnPlay.isEnabled = false
                    binding.btnPlay.alpha = 0.5f
                }

                binding.btnRemoveGuest.setOnClickListener {
                    db.collection("PartidaDuo").document(idPartida).delete()
                }
            }
    }

    private fun irAPartida(idPartida: String) {
        val codigo = "ID_ESKATU"
        val intent = Intent(this, PartidaActivity::class.java)
        intent.putExtra(PartidaActivity.EXTRA_PARAM, codigo)
        db.collection("PartidaDuo").document(idPartida).delete()
        startActivity(intent)
        finish()
    }

    private fun setupVistasNuevoEmisor() {
        esemisor = true
        esreceptor = false
        binding.btnInviteFriend.visibility = View.VISIBLE
        binding.layoutGuestProfile.visibility = View.GONE
        binding.btnPlay.visibility = View.VISIBLE
        binding.btnPlay.isEnabled = false
        binding.btnPlay.alpha = 0.5f
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
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_friend_invite, parent, false)
            return InviteVH(v)
        }

        override fun onBindViewHolder(holder: InviteVH, position: Int) {
            val user = friends[position]
            val userId = user["id"] ?: ""
            holder.name.text = user["username"]

            val avatarNombre = user["avatarActual"] ?: "avadef"
            holder.avatar.setImageResource(getResIdFromName(this@DuoActivity, avatarNombre))

            // Escucha en tiempo real solo para el indicador visual
            realtimeDb.getReference("estado_usuarios").child(userId)
                .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val estado = snapshot.getValue(String::class.java) ?: "offline"
                        if (estado == "online") {
                            holder.status.text = "● Online"
                            holder.status.setTextColor(Color.parseColor("#2ECC71"))
                            holder.btn.isEnabled = true
                            holder.btn.alpha = 1.0f
                        } else {
                            holder.status.text = "○ Offline"
                            holder.status.setTextColor(Color.GRAY)
                            holder.btn.isEnabled = false
                            holder.btn.alpha = 0.5f
                        }
                    }

                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                })

            holder.btn.setOnClickListener {
                invitarAmigo(userId)
            }
        }

        private fun invitarAmigo(userid: String) {
            val nuevaPartidaRef = db.collection("PartidaDuo").document()
            val idPartidaGenerada = nuevaPartidaRef.id

            val datosPartida = mapOf(
                "idemisor" to currentUser,
                "idreceptor" to userid,
                "onartua" to false,
                "jokatu" to false
            )

            nuevaPartidaRef.set(datosPartida).addOnSuccessListener {
                Toast.makeText(this@DuoActivity, "Gonbidapena bidali da!", Toast.LENGTH_SHORT)
                    .show()
                idPartidaActiva = idPartidaGenerada
                esemisor = true
                escucharComoEmisor(idPartidaGenerada)
                onInvite(userid)
            }
        }

        override fun getItemCount() = friends.size
    }
}