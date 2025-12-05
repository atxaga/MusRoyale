// kotlin
package com.example.musroyale

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.musroyale.databinding.FragmentPartidaBinding

class PartidaFragment: Fragment() {
    private lateinit var binding: FragmentPartidaBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPartidaBinding.inflate(inflater, container, false)

        binding.containerCard.setOnClickListener {
            val intent = Intent(requireContext(), MatchSetupActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }
}

