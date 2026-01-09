package com.example.musroyale

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musroyale.databinding.ActivityChatSplitBinding
import java.text.SimpleDateFormat
import java.util.*
class ChatSplitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatSplitBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var friendsAdapter: FriendsChatAdapter

    private val messagesList = mutableListOf<ChatMessage>()
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    private var currentUserId: String? = null
    private var selectedFriendId: String? = null // El ID del amigo con el que hablas
    private var chatListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatSplitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Obtener mi ID de usuario
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        currentUserId = prefs.getString("userRegistrado", null)

        setupFriendsList()
        setupChatArea()
        cargarAmigosDelDrawer()

        binding.btnMenuFriends.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
        binding.btnCloseDrawer.setOnClickListener { binding.drawerLayout.closeDrawer(GravityCompat.START) }
        binding.btnExit.setOnClickListener { finish() }

        // 2. Botón enviar ahora usa la base de datos real
        binding.btnSend.setOnClickListener { sendMessage() }
    }

    private fun setupFriendsList() {
        friendsAdapter = FriendsChatAdapter(mutableListOf()) { friend ->
            selectedFriendId = friend.id
            escucharChatReal(friend) // Cargamos el chat real al pulsar un amigo
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        binding.recyclerFriends.layoutManager = LinearLayoutManager(this)
        binding.recyclerFriends.adapter = friendsAdapter
    }

    private fun escucharChatReal(friend: Friend) {
        binding.chatTitleName.text = friend.name
        chatListener?.remove()

        chatListener = db.collection("Chats")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                val batch = db.batch()
                var hayCambiosPorMarcar = false

                messagesList.clear()
                snapshot?.documents?.forEach { doc ->
                    val emisor = doc.getString("idemisor") ?: ""
                    val receptor = doc.getString("idreceptor") ?: ""
                    val leido = doc.getBoolean("leido") ?: false

                    // 1. Filtrado de mensajes para mostrar en pantalla
                    if ((emisor == currentUserId && receptor == friend.id) ||
                        (emisor == friend.id && receptor == currentUserId)) {

                        messagesList.add(ChatMessage(
                            senderId = emisor,
                            receiverId = receptor,
                            message = doc.getString("mensaje") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isMine = emisor == currentUserId
                        ))

                        // 2. LÓGICA CLAVE: Si el mensaje es para MÍ, viene de ESTE AMIGO y no está LEÍDO...
                        if (receptor == currentUserId && emisor == friend.id && !leido) {
                            // ...lo preparamos para marcar como leído en el servidor
                            batch.update(doc.reference, "leido", true)
                            hayCambiosPorMarcar = true
                        }
                    }
                }

                // 3. Ejecutamos la actualización en Firebase si hay mensajes nuevos
                if (hayCambiosPorMarcar) {
                    batch.commit().addOnSuccessListener {
                        // Opcional: Log o acción tras marcar como leído
                    }
                }

                chatAdapter.notifyDataSetChanged()
                if (messagesList.isNotEmpty()) {
                    binding.recyclerChat.scrollToPosition(messagesList.size - 1)
                }
            }
    }

    private fun sendMessage() {
        val text = binding.editMessage.text.toString().trim()
        val fId = selectedFriendId

        if (text.isNotEmpty() && currentUserId != null && fId != null) {
            var messageData: HashMap<String, Any> = hashMapOf(
                "idemisor" to currentUserId!!,
                "idreceptor" to fId,
                "mensaje" to text,
                "timestamp" to System.currentTimeMillis(),
                "leido" to false
            )

            db.collection("Chats")
                .add(messageData)
                .addOnSuccessListener {
                    binding.editMessage.setText("")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("FirestoreError", "Error al enviar", e)
                }
        }
    }

    private fun setupChatArea() {
        chatAdapter = ChatAdapter(messagesList)
        binding.recyclerChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerChat.adapter = chatAdapter
    }

    private var friendsListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun cargarAmigosDelDrawer() {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentUserEmail = prefs.getString("userRegistrado", null) ?: return

        friendsListener = db.collection("Users").document(currentUserEmail)
            .addSnapshotListener { document, _ ->
                val amigosIds = document?.get("amigos") as? List<String> ?: listOf()

                if (amigosIds.isNotEmpty()) {
                    db.collection("Users")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), amigosIds)
                        .get()
                        .addOnSuccessListener { query ->
                            val listaAmigos = query.documents.map {
                                Friend(it.id, it.getString("username") ?: "Usuario",0, it.getString("avatarActual") ?: "avadef.png", )
                            }

                            // --- INICIO LÓGICA DE CONTEO ---
                            for (friend in listaAmigos) {
                                db.collection("Chats")
                                    .whereEqualTo("idemisor", friend.id)
                                    .whereEqualTo("idreceptor", currentUserId)
                                    .whereEqualTo("leido", false)
                                    .addSnapshotListener { snapshots, _ ->
                                        val count = snapshots?.size() ?: 0
                                        friend.unreadCount = count
                                        friendsAdapter.notifyDataSetChanged()
                                    }
                            }
                            // --- FIN LÓGICA DE CONTEO ---

                            friendsAdapter.updateData(listaAmigos)
                        }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatListener?.remove() // Importante limpiar al salir de la pantalla
    }
}