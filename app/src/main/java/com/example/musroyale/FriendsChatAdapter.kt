package com.example.musroyale

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musroyale.databinding.ItemFriendChatBinding

class FriendsChatAdapter(
    private val friends: List<Friend>,
    private val onClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendsChatAdapter.FriendViewHolder>() {

    // Variable para saber cuál está seleccionado y pintarlo diferente
    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendViewHolder(binding)
    }

    override fun getItemCount(): Int = friends.size

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.binding.txtName.text = friend.name
        holder.binding.txtStatus.text = friend.status

        // Cambiar color si está seleccionado
        if (selectedPosition == position) {
            holder.itemView.setBackgroundColor(Color.parseColor("#1F4E38")) // Verde seleccionado
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        holder.binding.onlineIndicator.visibility = if (friend.isOnline) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            selectedPosition = holder.bindingAdapterPosition
            notifyDataSetChanged() // Refrescar lista para actualizar selección
            onClick(friend)
        }
    }

    class FriendViewHolder(val binding: ItemFriendChatBinding) : RecyclerView.ViewHolder(binding.root)
}