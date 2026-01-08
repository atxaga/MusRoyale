package com.example.musroyale

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musroyale.databinding.ItemChatMessageBinding

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    inner class ChatViewHolder(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            if (message.isMine) {
                // MENSAJE ENVIADO (Derecha)
                binding.textSent.text = message.message // Usamos .message
                binding.textSent.visibility = View.VISIBLE
                binding.textReceived.visibility = View.GONE
            } else {
                // MENSAJE RECIBIDO (Izquierda)
                binding.textReceived.text = message.message // Usamos .message
                binding.textReceived.visibility = View.VISIBLE
                binding.textSent.visibility = View.GONE
            }
        }
    }
}