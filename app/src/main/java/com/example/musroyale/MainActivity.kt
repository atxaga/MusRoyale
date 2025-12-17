package com.example.musroyale

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.musroyale.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var tabList: List<FrameLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Cargar HomeFragment en el contenedor central (si es la primera vez)


        // 2. Inicializar lista de pestañas del Footer
        tabList = listOf(
            binding.tabAvatar,
            binding.tabChat,
            binding.tabPlay,
            binding.tabFriends,
            binding.tabStore
        )

        // 3. Configurar Listeners del Footer
        setupFooterListeners()

        // 4. Marcar la pestaña "Play" como seleccionada por defecto al entrar
        selectTab(binding.tabPlay)
    }

    private fun setupFooterListeners() {
        // --- AVATAR ---
        binding.tabAvatar.setOnClickListener {
            selectTab(binding.tabAvatar)
            // startActivity(Intent(this, AvatarActivity::class.java))
        }

        // --- CHAT (Abre otra Activity) ---
        binding.tabChat.setOnClickListener {
            selectTab(binding.tabChat)
            val intent = Intent(this, ChatSplitActivity::class.java)
            // Opcional: Animation transitions
            startActivity(intent)
        }

        // --- PLAY/HOME (Se queda aquí) ---
        binding.tabPlay.setOnClickListener {
            selectTab(binding.tabPlay)
            // Como ya estamos en MainActivity, no hacemos startActivity.
            // Opcional: Si quisieras reiniciar el fragmento home:
            // supportFragmentManager.beginTransaction().replace(R.id.mainContainer, HomeFragment()).commit()
        }

        // --- FRIENDS (Abre otra Activity) ---
        binding.tabFriends.setOnClickListener {
            selectTab(binding.tabFriends)
            startActivity(Intent(this, FriendsActivity::class.java))
        }

        // --- STORE (Abre otra Activity) ---
        binding.tabStore.setOnClickListener {
            selectTab(binding.tabStore)
            startActivity(Intent(this, StoreActivity::class.java))
        }
    }

    private fun selectTab(selectedTab: FrameLayout?) {
        val moveUpY = -25f

        for ((index, tab) in tabList.withIndex()) {
            val iconImage = tab.getChildAt(0) as ImageView

            if (tab == selectedTab) {
                // --- SELECCIONADO ---
                // Asignar fondo según posición (Izquierda, Derecha o Centro)
                when (index) {
                    0 -> tab.setBackgroundResource(R.drawable.bg_tab_selected_left)
                    tabList.size - 1 -> tab.setBackgroundResource(R.drawable.bg_tab_selected_right)
                    else -> tab.setBackgroundResource(R.drawable.bg_tab_selected_center)
                }

                // Animación Subir
                iconImage.animate()
                    .translationY(moveUpY)
                    .setDuration(200)
                    .start()

            } else {
                // --- NO SELECCIONADO ---
                tab.background = null

                // Animación Bajar
                iconImage.animate()
                    .translationY(0f)
                    .setDuration(200)
                    .start()
            }
        }
    }
}