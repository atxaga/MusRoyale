package com.example.musroyale

import androidx.fragment.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.musroyale.databinding.FragmentFooterBinding



class FooterFragment: Fragment() {
    private lateinit var binding: FragmentFooterBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFooterBinding.inflate(inflater, container, false)
        binding.btnChat.setOnClickListener {
            var intent = Intent(activity, ChatSplitActivity::class.java)
            startActivity(intent)
        }

        // btnFriends -> abrir FriendsActivity
        binding.btnFriends.setOnClickListener {
            val intent = Intent(activity, FriendsActivity::class.java)
            startActivity(intent)
        }

        // btnStore -> abrir StoreActivity
        binding.root.findViewById<View>(R.id.btnStore)?.setOnClickListener {
            val intent = Intent(activity, StoreActivity::class.java)
            startActivity(intent)
        }

        // btnPlay -> ir a la pantalla principal (MainActivity)
        binding.root.findViewById<View>(R.id.btnPlay)?.setOnClickListener {
            val intent = Intent(activity, MainActivity::class.java)
            // clear top para evitar stacks múltiples
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }


        return binding.root
    }
}