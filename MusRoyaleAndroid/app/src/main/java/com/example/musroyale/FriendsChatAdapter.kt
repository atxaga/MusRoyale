package com.example.musroyale

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musroyale.databinding.ItemFriendChatBinding

class FriendsChatAdapter(
    private var friends: MutableList<Friend>,
    private val onClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendsChatAdapter.FriendVH>() {

    // Cambiamos a ViewBinding para acceder fácilmente a todos los IDs
    class FriendVH(val binding: ItemFriendChatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendVH {
        val binding = ItemFriendChatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FriendVH(binding)
    }

    override fun onBindViewHolder(holder: FriendVH, position: Int) {
        val friend = friends[position]
        val context = holder.itemView.context

        holder.binding.txtName.text = friend.name

        // --- LÓGICA DEL AVATAR ---
        // 1. Limpiamos el nombre (ej: "ava1.png" -> "ava1")
        val cleanName = friend.avatar.replace(".png", "")

        // 2. Buscamos el ID del recurso
        val resId = context.resources.getIdentifier(cleanName, "drawable", context.packageName)

        // 3. Lo aplicamos al ImageView (asegúrate de que el ID en el XML sea imgFriendAvatar)
        if (resId != 0) {
            holder.binding.imgAvatar.setImageResource(resId)
        } else {
            holder.binding.imgAvatar.setImageResource(R.drawable.ic_avatar3) // Fallback
        }

        // --- LÓGICA DEL BADGE ---
        if (friend.unreadCount > 0) {
            holder.binding.badgeCount.visibility = View.VISIBLE
            holder.binding.badgeCount.text = friend.unreadCount.toString()
        } else {
            holder.binding.badgeCount.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(friend) }
    }

    override fun getItemCount() = friends.size

    fun updateData(newList: List<Friend>) {
        friends.clear()
        friends.addAll(newList)
        notifyDataSetChanged()
    }
}