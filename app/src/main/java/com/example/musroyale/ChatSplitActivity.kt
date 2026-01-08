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

        // Cerramos el listener anterior si existía
        chatListener?.remove()

        // Escuchamos la colección "Chats" filtrada por los dos usuarios
        chatListener = db.collection("Chats")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                messagesList.clear()
                snapshot?.documents?.forEach { doc ->
                    val emisor = doc.getString("idemisor") ?: ""
                    val receptor = doc.getString("idreceptor") ?: ""

                    // Filtrado: Solo mensajes entre YO y el AMIGO seleccionado
                    if ((emisor == currentUserId && receptor == friend.id) ||
                        (emisor == friend.id && receptor == currentUserId)) {

                        messagesList.add(ChatMessage(
                            senderId = emisor,
                            receiverId = receptor,
                            message = doc.getString("mensaje") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isMine = emisor == currentUserId
                        ))
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
            val messageData = mapOf(
                "idemisor" to currentUserId,
                "idreceptor" to fId,
                "mensaje" to text,
                "timestamp" to System.currentTimeMillis()
            )

            // Guardamos en Firestore
            db.collection("Chats").add(messageData).addOnSuccessListener {
                binding.editMessage.setText("") // El listener lo pintará automáticamente
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

        // Escuchamos MI documento para ver si mi lista de "amigos" cambia
        friendsListener = db.collection("Users").document(currentUserEmail)
            .addSnapshotListener { document, error ->
                if (error != null) return@addSnapshotListener

                val amigosIds = document?.get("amigos") as? List<String> ?: listOf()

                if (amigosIds.isNotEmpty()) {
                    // Buscamos los datos de esos IDs
                    db.collection("Users")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), amigosIds)
                        .get()
                        .addOnSuccessListener { query ->
                            val listaAmigos = query.documents.map {
                                Friend(it.id, it.getString("username") ?: "Usuario")
                            }
                            friendsAdapter.updateData(listaAmigos)
                        }
                } else {
                    // Si no tienes amigos, vaciamos la lista
                    friendsAdapter.updateData(mutableListOf())
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatListener?.remove() // Importante limpiar al salir de la pantalla
    }
}