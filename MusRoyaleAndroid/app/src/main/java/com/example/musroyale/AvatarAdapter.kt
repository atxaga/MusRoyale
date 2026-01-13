package com.example.musroyale

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView

class AvatarAdapter(
    private val avatares: List<String>,
    private val avatarActual: String,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    class AvatarViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val ivAvatar: ShapeableImageView = v.findViewById(R.id.ivAvatarItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_avatar_circle, parent, false)
        return AvatarViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        val nombreDesdeDB = avatares[position] // Ej: "rey.png"
        val context = holder.itemView.context

        // 1. Quitamos el .png solo para buscar el recurso local
        val cleanName = nombreDesdeDB.replace(".png", "")
        val resId = context.resources.getIdentifier(cleanName, "drawable", context.packageName)

        if (resId != 0) {
            holder.ivAvatar.setImageResource(resId)
        }

        // 2. Comparamos el nombre completo (con .png) para resaltar el actual
        if (nombreDesdeDB == avatarActual) {
            holder.ivAvatar.strokeWidth = 10f
            holder.ivAvatar.strokeColor = ColorStateList.valueOf(Color.parseColor("#FFD700"))
        } else {
            holder.ivAvatar.strokeWidth = 3f
            holder.ivAvatar.strokeColor = ColorStateList.valueOf(Color.parseColor("#D2B48C"))
        }

        holder.itemView.setOnClickListener { onClick(nombreDesdeDB) }
    }

    override fun getItemCount() = avatares.size
}