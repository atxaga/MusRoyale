package com.example.musroyale

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.musroyale.databinding.FragmentFooterBinding

class FooterFragment: Fragment() {
    private var _binding: FragmentFooterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFooterBinding.inflate(inflater, container, false)

        // btnFriends -> abrir FriendsActivity
        binding.root.findViewById<View>(R.id.btnFriends)?.setOnClickListener {
            val intent = Intent(requireContext(), FriendsActivity::class.java)
            startActivity(intent)
        }

        // btnPlay -> ir a la pantalla principal (MainActivity)
        binding.root.findViewById<View>(R.id.btnPlay)?.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            // clear top para evitar stacks múltiples
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}