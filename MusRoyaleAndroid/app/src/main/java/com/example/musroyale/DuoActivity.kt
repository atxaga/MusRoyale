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
    private lateinit var currentUser: String
    private val realtimeDb = FirebaseDatabase.getInstance("https://musroyale-488aa-default-rtdb.europe-west1.firebasedatabase.app/")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDuosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        currentUser = prefs.getString("userRegistrado", null) ?: ""

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
        decidirRolYEscuchar()
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
    private var esemisor = false
    private var esreceptor = false
    private var idPartidaActiva: String? = null

    private fun decidirRolYEscuchar() {
        val currentUserId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("userRegistrado", null) ?: return

        // Escuchamos TODA la colección buscando partidas donde participemos y no hayan empezado
        db.collection("PartidaDuo")
            .whereIn("idreceptor", listOf(currentUserId)) // Buscamos si somos receptores
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                // 1. Lógica para determinar si somos RECEPTORES
                val partidaReceptor = snapshots?.documents?.find {
                    it.getString("idreceptor") == currentUserId && it.getBoolean("jokatu") == false
                }

                if (partidaReceptor != null) {
                    esreceptor = true
                    esemisor = false
                    idPartidaActiva = partidaReceptor.id
                    escucharComoReceptor(idPartidaActiva!!)
                    return@addSnapshotListener // Salimos para no ejecutar la lógica de emisor
                }

                // 2. Si no somos receptores, buscamos si somos EMISORES
                db.collection("PartidaDuo")
                    .whereEqualTo("idemisor", currentUserId)
                    .whereEqualTo("jokatu", false)
                    .addSnapshotListener { emisorSnaps, _ ->
                        val partidaEmisor = emisorSnaps?.documents?.firstOrNull()

                        if (partidaEmisor != null) {
                            esemisor = true
                            esreceptor = false
                            idPartidaActiva = partidaEmisor.id
                            escucharComoEmisor(idPartidaActiva!!)
                        } else {
                            // CASO NUEVO: No hay nada en la DB, somos emisor libre
                            setupVistasNuevoEmisor()
                        }
                    }
            }
    }

    private fun escucharComoReceptor(idPartida: String) {
        db.collection("PartidaDuo").document(idPartida)
            .addSnapshotListener { doc, e ->
                if (e != null || doc == null || !doc.exists()) return@addSnapshotListener

                // El receptor siempre tiene el Play oculto
                binding.btnPlay.visibility = View.GONE
                binding.btnInviteFriend.visibility = View.GONE
                binding.layoutGuestProfile.visibility = View.VISIBLE

                val onartua = doc.getBoolean("onartua") ?: false
                val jokatu = doc.getBoolean("jokatu") ?: false

                if (onartua) {
                    // Buscamos nombre del emisor (puedes guardarlo en el doc al crear para ahorrar esta consulta)
                    val emisorId = doc.getString("idemisor") ?: ""
                    db.collection("Users").document(emisorId).get().addOnSuccessListener { u ->
                        binding.tvGuestName.text = u.getString("username") ?: "Laguna"
                        // Aquí cargarías el avatar con getResIdFromName
                    }

                    // Si el emisor pulsa JOKATU, el receptor salta
                    if (jokatu) {
                        val codigoExistente = doc.getString("kodea") ?: ""
                        irAPartida(idPartida, codigoExistente)
                    }
                } else {
                    binding.tvGuestName.text = "Zure lagunaren zain..."
                }
            }
    }

    private fun escucharComoEmisor(idPartida: String) {
        db.collection("PartidaDuo").document(idPartida)
            .addSnapshotListener { doc, e ->
                if (e != null || doc == null || !doc.exists()) return@addSnapshotListener

                val onartua = doc.getBoolean("onartua") ?: false

                if (onartua) {
                    // El amigo ha aceptado la invitación
                    binding.btnInviteFriend.visibility = View.GONE
                    binding.layoutGuestProfile.visibility = View.VISIBLE

                    val idReceptor = doc.getString("idreceptor") ?: ""
                    db.collection("Users").document(idReceptor).get().addOnSuccessListener { u ->
                        binding.tvGuestName.text = u.getString("username") ?: "Laguna"
                    }

                    // Habilitamos el botón JOKATU
                    binding.btnPlay.visibility = View.VISIBLE
                    binding.btnPlay.isEnabled = true
                    binding.btnPlay.alpha = 1.0f
                    binding.btnPlay.setOnClickListener {
                        val codigoEnDB = doc.getString("kodea") ?: ""
                        doc.reference.update("jokatu", true)
                            .addOnSuccessListener {
                                irAPartida(idPartida)
                            }
                    }
                } else {
                    // Esperando a que el receptor acepte el diálogo en su MainActivity
                    binding.btnInviteFriend.visibility = View.GONE // Ya invitamos, ocultamos el botón
                    binding.layoutGuestProfile.visibility = View.VISIBLE
                    binding.tvGuestName.text = "Gonbidapena bidalita..."
                    binding.btnPlay.isEnabled = false
                    binding.btnPlay.alpha = 0.5f
                }
            }
    }

    private fun irAPartida(idPartida: String) {
        var codigo = "ID_ESKATU"
        val intent = Intent(this, PartidaActivity::class.java).apply {
            putExtra("idPartida", idPartida)
            putExtra("EXTRA_CODE", codigo)
            putExtra("EXTRA_PARAM", "UNIRSE_PRIVADA")
        }
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

            holder.btn.setOnClickListener {invitarAmigo(userId)  }
        }
        fun invitarAmigo(userid: String) {
                db.collection("PartidaDuo").document()
                    .set(
                        mapOf(
                            "idemisor" to currentUser,
                            "idreceptor" to userid,
                            "onartua" to false,
                            "jokatu" to false,
                        )
                    ).addOnSuccessListener {
                        Toast.makeText(this@DuoActivity, "Gonbidapena bidali da!", Toast.LENGTH_SHORT).show()
                        onInvite(userid)
                    }


        }

        override fun getItemCount() = friends.size
    }
}