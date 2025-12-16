package com.example.musroyale

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musroyale.databinding.ItemGameModeBinding


class GameModeAdapter(
    private val modes: List<GameMode>
) : RecyclerView.Adapter<GameModeAdapter.ModeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModeViewHolder {
        val binding = ItemGameModeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ModeViewHolder(binding)
    }

    override fun getItemCount(): Int = modes.size

    override fun onBindViewHolder(holder: ModeViewHolder, position: Int) {
        holder.bind(modes[position])
    }

    inner class ModeViewHolder(private val binding: ItemGameModeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mode: GameMode) {
            binding.imageGameMode.setImageResource(mode.imageRes)
            binding.root.setOnClickListener {
                mode.function()
            }
            }


        }
        }



