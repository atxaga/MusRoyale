// kotlin
package com.example.musroyale

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.musroyale.databinding.FragmentDuoBinding

class DuoFragment: Fragment() {
    private lateinit var binding: FragmentDuoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDuoBinding.inflate(inflater, container, false)

        binding.containerCard.setOnClickListener {
            val intent = Intent(requireContext(), DuoActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }
}

