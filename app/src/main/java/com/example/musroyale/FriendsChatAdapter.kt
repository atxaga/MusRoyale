package com.example.musroyale

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musroyale.databinding.ItemFriendChatBinding

class FriendsChatAdapter(
    private var friends: MutableList<Friend>,
    private val onClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendsChatAdapter.FriendVH>() {

    class FriendVH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val nameText: android.widget.TextView = view.findViewById(R.id.txtName)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FriendVH {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_chat, parent, false) // Crea un layout simple para esto
        return FriendVH(view)
    }

    override fun onBindViewHolder(holder: FriendVH, position: Int) {
        val friend = friends[position]
        holder.nameText.text = friend.name
        holder.itemView.setOnClickListener { onClick(friend) }
    }

    override fun getItemCount() = friends.size

    fun updateData(newList: List<Friend>) {
        friends.clear()
        friends.addAll(newList)
        notifyDataSetChanged()
    }
}