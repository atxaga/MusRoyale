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
                // Es mi mensaje: Muestro el de la derecha, oculto el de la izquierda
                binding.textSent.text = message.text
                binding.textSent.visibility = View.VISIBLE
                binding.textReceived.visibility = View.GONE
            } else {
                // Es su mensaje: Muestro el de la izquierda, oculto el de la derecha
                binding.textReceived.text = message.text
                binding.textReceived.visibility = View.VISIBLE
                binding.textSent.visibility = View.GONE
            }
        }
    }
}