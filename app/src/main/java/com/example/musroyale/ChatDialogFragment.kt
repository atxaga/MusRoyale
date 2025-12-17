package com.example.musroyale

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musroyale.databinding.DialogChatBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatDialogFragment : DialogFragment() {

    private var _binding: DialogChatBinding? = null
    private val binding get() = _binding!!

    // Lista estática temporal para no perder mensajes al cerrar/abrir
    // (En una app real esto iría en un ViewModel)
    companion object {
        val messagesList = mutableListOf<ChatMessage>()
    }

    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogChatBinding.inflate(inflater, container, false)

        // Hacemos el fondo del sistema transparente para que se vean nuestros bordes redondeados
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar RecyclerView
        adapter = ChatAdapter(messagesList)
        binding.recyclerChat.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
        }
        binding.recyclerChat.adapter = adapter

        // Botón Cerrar
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Botón Enviar
        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        // Si hay mensajes, ir al último
        if (messagesList.isNotEmpty()) {
            binding.recyclerChat.scrollToPosition(messagesList.size - 1)
        }
    }

    override fun onStart() {
        super.onStart()
        // Obtener dimensiones de la pantalla
        val displayMetrics = resources.displayMetrics

        // Calcular tamaño: 90% del ancho y 85% del alto de la pantalla
        val width = (displayMetrics.widthPixels * 0.90).toInt()
        val height = (displayMetrics.heightPixels * 0.65).toInt()

        // Aplicar el tamaño a la ventana del diálogo
        dialog?.window?.setLayout(width, height)
    }

    private fun sendMessage() {
        val text = binding.editMessage.text.toString().trim()
        if (text.isNotEmpty()) {
            val myMessage = ChatMessage(text, true, getCurrentTime())
            addMessageToChat(myMessage)
            binding.editMessage.setText("")
            simulateFriendReply()
        }
    }

    private fun addMessageToChat(message: ChatMessage) {
        messagesList.add(message)
        adapter.notifyItemInserted(messagesList.size - 1)
        binding.recyclerChat.smoothScrollToPosition(messagesList.size - 1)
    }

    private fun simulateFriendReply() {
        Handler(Looper.getMainLooper()).postDelayed({
            // Verificar si el fragmento sigue activo para evitar crashes
            if (!isAdded) return@postDelayed

            val replies = listOf("¡Voy!", "Paso", "Envido más", "No quiero", "¿Qué llevas?")
            val friendMessage = ChatMessage(replies.random(), false, getCurrentTime())
            addMessageToChat(friendMessage)
        }, 1000)
    }

    private fun getCurrentTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}