package com.example.musroyale

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.musroyale.databinding.FragmentFooterBinding

class FooterFragment : Fragment() {

    private var _binding: FragmentFooterBinding? = null
    private val binding get() = _binding!!

    private lateinit var tabList: List<FrameLayout>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFooterBinding.inflate(inflater, container, false)

        // 1. Definimos la lista de TABS (Contenedores FrameLayout)
        tabList = listOf(
            binding.tabAvatar,
            binding.tabChat,
            binding.tabPlay,
            binding.tabFriends,
            binding.tabStore
        )

        // 2. Configurar Listeners (Visual + Navegación juntos)
        setupListeners()

        // 3. (Opcional) Marcar el botón de la actividad actual automáticamente
        highlightCurrentActivity()

        return binding.root
    }

    private fun setupListeners() {
        // --- AVATAR ---
        binding.tabAvatar.setOnClickListener {
            selectTab(binding.tabAvatar)
            // Agrega aquí tu intent para Avatar si tienes uno
            // startActivity(Intent(activity, AvatarActivity::class.java))
        }

        // --- CHAT ---
        binding.tabChat.setOnClickListener {
            selectTab(binding.tabChat)
            if (activity !is ChatSplitActivity) { // Evitar recargar si ya estás ahí
                startActivity(Intent(activity, ChatSplitActivity::class.java))
            }
        }

        // --- PLAY (HOME) ---
        binding.tabPlay.setOnClickListener {
            selectTab(binding.tabPlay)
            if (activity !is MainActivity) {
                val intent = Intent(activity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        }

        // --- FRIENDS ---
        binding.tabFriends.setOnClickListener {
            selectTab(binding.tabFriends)
            if (activity !is FriendsActivity) {
                startActivity(Intent(activity, FriendsActivity::class.java))
            }
        }

        // --- STORE ---
        binding.tabStore.setOnClickListener {
            selectTab(binding.tabStore)
            if (activity !is StoreActivity) {
                startActivity(Intent(activity, StoreActivity::class.java))
            }
        }
    }

    // Esta función detecta en qué pantalla estás y marca el icono al cargar
    private fun highlightCurrentActivity() {
        val currentActivity = requireActivity()

        when (currentActivity) {
            is MainActivity -> selectTab(binding.tabPlay)
            is ChatSplitActivity -> selectTab(binding.tabChat)
            is FriendsActivity -> selectTab(binding.tabFriends)
            is StoreActivity -> selectTab(binding.tabStore)
            // is AvatarActivity -> selectTab(binding.tabAvatar)
            else -> {
                // Si no coincide con ninguna, deseleccionamos todos o marcamos uno por defecto
                // selectTab(null)
            }
        }
    }

    private fun selectTab(selectedTab: FrameLayout?) {
        val moveUpY = -25f

        for ((index, tab) in tabList.withIndex()) {
            val iconImage = tab.getChildAt(0) as ImageView

            if (tab == selectedTab) {
                // --- SELECCIONADO ---
                when (index) {
                    0 -> tab.setBackgroundResource(R.drawable.bg_tab_selected_left)
                    tabList.size - 1 -> tab.setBackgroundResource(R.drawable.bg_tab_selected_right)
                    else -> tab.setBackgroundResource(R.drawable.bg_tab_selected_center)
                }

                iconImage.animate()
                    .translationY(moveUpY)
                    .setDuration(200)
                    .start()
            } else {
                // --- NO SELECCIONADO ---
                tab.background = null
                iconImage.animate()
                    .translationY(0f)
                    .setDuration(200)
                    .start()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}