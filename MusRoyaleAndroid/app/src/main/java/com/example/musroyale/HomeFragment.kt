package com.example.musroyale

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.musroyale.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    // Binding variable (nullable para manejar el ciclo de vida del fragmento)
    private var _binding: FragmentHomeBinding? = null
    // Propiedad válida solo entre onCreateView y onDestroyView
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflamos el layout (asegúrate de que tu xml se llame fragment_home.xml)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Definimos los modos de juego
        // NOTA: Usamos requireContext() o requireActivity() en lugar de 'this' para los Intents
        val modes = listOf(
            GameMode(R.drawable.partida_azkarra) {
                startActivity(Intent(requireContext(), MatchSetupActivity::class.java))
            },
            GameMode(R.drawable.parejak) {
                startActivity(Intent(requireContext(), DuoActivity::class.java))
            },
            GameMode(R.drawable.pribatua) {
                startActivity(Intent(requireContext(), PrivateMatchSetupActivity::class.java))
            }
        )

        // Configuración del RecyclerView
        val adapter = GameModeAdapter(modes)
        binding.modesRecycler.adapter = adapter
        binding.modesRecycler.setHasFixedSize(true)

        // SnapHelper para centrar los items
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.modesRecycler)

        // LayoutManager horizontal
        binding.modesRecycler.layoutManager = LinearLayoutManager(
            requireContext(), // Usamos requireContext() aquí
            RecyclerView.HORIZONTAL,
            false
        )

        // Decoración (espaciado)
        binding.modesRecycler.addItemDecoration(ModesSpacingDecoration(16))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Importante: Limpiar el binding para evitar memory leaks
        _binding = null
    }
}