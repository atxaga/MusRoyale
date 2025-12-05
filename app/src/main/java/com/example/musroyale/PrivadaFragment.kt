// kotlin
package com.example.musroyale

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.musroyale.databinding.FragmentPrivadaBinding

class PrivadaFragment: Fragment() {
    private lateinit var binding: FragmentPrivadaBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPrivadaBinding.inflate(inflater, container, false)

        binding.containerCard.setOnClickListener {
            val intent = Intent(requireContext(), PrivateMatchSetupActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }
}

