package com.example.musroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.musroyale.databinding.FragmentHeaderPartidaBinding

class HeaderPartidaFragment : Fragment() {
    private var _binding: FragmentHeaderPartidaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHeaderPartidaBinding.inflate(inflater, container, false)

        // Listener para el icono de ajustes en el header
        binding.buttonSettings.setOnClickListener {
            Toast.makeText(requireContext(), "Ajustes (placeholder)", Toast.LENGTH_SHORT).show()
            // TODO: abrir Activity/Fragment de ajustes
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setLeftTeam(name: String) {
        binding.leftTeamName.text = name
    }

    // Establece las dos casillas de puntuación del equipo izquierdo (p.ej. "1" y "4")
    fun setLeftScoreBoxes(first: Int, second: Int) {
        binding.leftScoreBox1.text = first.toString()
        binding.leftScoreBox2.text = second.toString()
    }

    fun setRightTeam(name: String) {
        binding.rightTeamName.text = name
    }

    // Establece las dos casillas de puntuación del equipo derecho
    fun setRightScoreBoxes(first: Int, second: Int) {
        binding.rightScoreBox1.text = first.toString()
        binding.rightScoreBox2.text = second.toString()
    }


}
