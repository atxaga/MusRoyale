package com.example.musroyale

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musroyale.databinding.ItemChatMessageBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val currentMessage = messages[position]
        val previousMessage = if (position > 0) messages[position - 1] else null
        holder.bind(currentMessage, previousMessage)
    }

    inner class ChatViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage, previousMessage: ChatMessage?) {
            // --- 1. Lógica del Separador de Fecha ---
            val dateText = getFormattedDateSeparator(message.timestamp, previousMessage?.timestamp)
            if (dateText != null) {
                binding.textDateSeparator.visibility = View.VISIBLE
                binding.textDateSeparator.text = dateText
            } else {
                binding.textDateSeparator.visibility = View.GONE
            }

            // --- 2. Formateo de la hora ---
            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeString = sdfTime.format(Date(message.timestamp))

            // --- 3. Lógica de visibilidad y Ticks ---
            if (message.isMine) {
                binding.layoutSent.visibility = View.VISIBLE
                binding.layoutReceived.visibility = View.GONE
                binding.textSent.text = message.message
                binding.timeSent.text = timeString

                // Cambio de imagen según el estado de lectura
                if (message.leido) {
                    binding.imgStatus.setImageResource(R.drawable.leido) // Doble tick azul
                } else {
                    binding.imgStatus.setImageResource(R.drawable.noleido) // Doble tick gris
                }
            } else {
                binding.layoutReceived.visibility = View.VISIBLE
                binding.layoutSent.visibility = View.GONE
                binding.textReceived.text = message.message
                binding.timeReceived.text = timeString
            }
        }
    }

    // Función para determinar si mostrar "Hoy", "Ayer" o la fecha
    private fun getFormattedDateSeparator(currentTs: Long, prevTs: Long?): String? {
        if (prevTs == null || !isSameDay(currentTs, prevTs)) {
            val now = Calendar.getInstance()
            val messageDate = Calendar.getInstance().apply { timeInMillis = currentTs }

            return when {
                isSameDay(currentTs, now.timeInMillis) -> "Hoy"
                isSameDay(currentTs, now.timeInMillis - 86400000) -> "Ayer"
                else -> {
                    val sdf = SimpleDateFormat("d 'de' MMMM", Locale("es", "ES"))
                    sdf.format(Date(currentTs))
                }
            }
        }
        return null
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}