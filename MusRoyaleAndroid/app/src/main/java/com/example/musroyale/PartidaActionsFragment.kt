package com.example.musroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.musroyale.databinding.FragmentPartidaActionsBinding

class PartidaActionsFragment : Fragment() {
    private var _binding: FragmentPartidaActionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPartidaActionsBinding.inflate(inflater, container, false)

        binding.buttonRaise.setOnClickListener {
            // TODO: conectar con lógica de subir apuesta
        }

        binding.buttonCall.setOnClickListener {
            // TODO: continuar con la jugada
        }

        binding.buttonFold.setOnClickListener {
            // TODO: abandonar mano actual
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

