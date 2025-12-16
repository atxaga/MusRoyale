package com.example.musroyale

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musroyale.databinding.ActivityChatSplitBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatSplitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatSplitBinding

    // Adaptadores
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var friendsAdapter: FriendsAdapter

    // Datos
    private val messagesList = mutableListOf<ChatMessage>()
    private val friendsList = listOf(
        Friend("Sala General", "Todos los jugadores", true),
        Friend("Iñaki", "Jugando...", true),
        Friend("Maite", "En línea", true),
        Friend("Jon", "Desconectado", false),
        Friend("Mikel", "En partida", true)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatSplitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ----------------------------------------------------
        // 1. ABRIR EL MENÚ (Al pulsar la hamburguesa)
        // ----------------------------------------------------
        binding.btnMenuFriends.setOnClickListener {
            // Abre el panel desde la izquierda (START)
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // ----------------------------------------------------
        // 2. CERRAR EL MENÚ (Al pulsar la X dentro del menú)
        // ----------------------------------------------------
        binding.btnCloseDrawer.setOnClickListener {
            // Cierra el panel
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Configuración de listas (el resto sigue igual)
        setupFriendsList()
        setupChatArea()

        // Botón salir
        binding.btnExit.setOnClickListener {
            finish()
        }
        // Botón enviar
        binding.btnSend.setOnClickListener { sendMessage() }
    }

    private fun setupFriendsList() {
        friendsAdapter = FriendsAdapter(friendsList) { friend ->
            // AL HACER CLICK EN UN AMIGO:
            loadChatForFriend(friend)
        }
        binding.recyclerFriends.layoutManager = LinearLayoutManager(this)
        binding.recyclerFriends.adapter = friendsAdapter
    }

    private fun setupChatArea() {
        chatAdapter = ChatAdapter(messagesList)
        binding.recyclerChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.recyclerChat.adapter = chatAdapter

        // Cargar por defecto la sala general
        loadChatForFriend(friendsList[0])
    }

    private fun loadChatForFriend(friend: Friend) {
        // 1. Cambiar Título
        binding.chatTitleName.text = friend.name

        // 2. Cambiar Estado
        binding.chatTitleStatus.text = if(friend.isOnline) "• En línea" else "• Offline"
        binding.chatTitleStatus.setTextColor(
            if(friend.isOnline) android.graphics.Color.parseColor("#4CAF50")
            else android.graphics.Color.GRAY
        )

        // 3. Limpiar mensajes anteriores (simulación)
        messagesList.clear()

        // 4. Añadir un mensaje de bienvenida ficticio
        messagesList.add(ChatMessage("Has entrado al chat con ${friend.name}", false, getCurrentTime()))
        chatAdapter.notifyDataSetChanged()
    }

    private fun sendMessage() {
        val text = binding.editMessage.text.toString().trim()
        if (text.isNotEmpty()) {
            val myMessage = ChatMessage(text, true, getCurrentTime())
            messagesList.add(myMessage)
            chatAdapter.notifyItemInserted(messagesList.size - 1)
            binding.recyclerChat.smoothScrollToPosition(messagesList.size - 1)

            binding.editMessage.setText("")

            // Simular respuesta
            Handler(Looper.getMainLooper()).postDelayed({
                messagesList.add(ChatMessage("Jajaja, ¡buena esa!", false, getCurrentTime()))
                chatAdapter.notifyItemInserted(messagesList.size - 1)
                binding.recyclerChat.smoothScrollToPosition(messagesList.size - 1)
            }, 1500)
        }
    }

    private fun getCurrentTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}