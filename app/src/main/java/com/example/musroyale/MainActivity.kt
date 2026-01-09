package com.example.musroyale

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.musroyale.databinding.ActivityMainBinding
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    data class TabItem(
        val layout: LinearLayout,
        val icon: ImageView,
        val text: TextView
    )

    private lateinit var tabs: List<TabItem>
    private var currentUserId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        currentUserId = prefs.getString("userRegistrado", null)

        // 1. Escucha de notificaciones de chat
        if (currentUserId != null) {
            escucharNotificacionesChat()
        }

        // 2. Setup de Tabs y Fragment inicial
        setupTabs()
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            selectTab(binding.tabPlay)
        }

        // 3. Listeners de Botones de Cabecera
        binding.btnLogout.setOnClickListener { logout() }

        // El botón de balance ahora lleva a la pantalla de Crypto
        binding.btnAddBalance.setOnClickListener {
            startActivity(Intent(this, CryptoPaymentActivity::class.java))
        }

        setupFooterListeners()
        cargarDatosUser() // Aquí dentro manejaremos la visibilidad del panel admin
    }


    override fun onResume() {
        super.onResume()
        cargarDatosUser()
    }
    fun logout(){
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        prefs.edit().remove("userRegistrado").apply()
        currentUserId = null;
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun cargarDatosUser() {
        if (currentUserId == null) return

        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("Users").document(currentUserId!!)

        userListener?.remove()
        userListener = docRef.addSnapshotListener { document, error ->
            if (error != null) return@addSnapshotListener

            if (document != null && document.exists()) {
                binding.loadingOverlay.visibility = View.GONE

                val username = document.getString("username") ?: "Usuario"
                val balance = document.get("dinero")?.toString() ?: "0"

                binding.txtUsername.text = username
                binding.txtBalance.text = balance

                // === LÓGICA DE ADMINISTRADOR SEGURA ===
                // Cambia "tu_correo@gmail.com" por tu correo real o ID de administrador
                if (currentUserId == "kHjrbXVjxZQzRHRvvqf7") {
                    binding.btnAdminPanel.visibility = View.VISIBLE
                    binding.btnAdminPanel.setOnClickListener {
                        startActivity(Intent(this, AdminActivity::class.java))
                    }
                } else {
                    binding.btnAdminPanel.visibility = View.GONE
                }
            }
        }
    }

    // Limpieza para evitar fugas de memoria
    override fun onDestroy() {
        super.onDestroy()
        userListener?.remove()
        chatNotificationsListener?.remove() // Limpiar también este
    }

    private fun setupFooterListeners() {
        binding.tabAvatar.setOnClickListener {
            selectTab(binding.tabAvatar)
            binding.header.visibility = View.VISIBLE
        }

        binding.tabChat.setOnClickListener {
            // Si el chat es otra activity, no necesitamos hacer selectTab aquí visualmente
            startActivity(Intent(this, ChatSplitActivity::class.java))
        }

        binding.tabPlay.setOnClickListener {
            selectTab(binding.tabPlay)
            loadFragment(HomeFragment())
            binding.header.visibility = View.VISIBLE
        }

        binding.tabFriends.setOnClickListener {
            selectTab(binding.tabFriends)
            loadFragment(FriendsFragment())
            binding.header.visibility = View.GONE
        }

        binding.tabStore.setOnClickListener {
            selectTab(binding.tabStore)
            loadFragment(StoreFragment())
            binding.header.visibility = View.GONE
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContainer, fragment)
            .commit()
    }
    private var chatNotificationsListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun escucharNotificacionesChat() {
        val uid = currentUserId ?: return
        val db = FirebaseFirestore.getInstance()

        // Escuchamos mensajes dirigidos a mí que no han sido leídos
        chatNotificationsListener = db.collection("Chats")
            .whereEqualTo("idreceptor", uid)
            .whereEqualTo("leido", false)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                val count = snapshots?.size() ?: 0

                if (count > 0) {
                    binding.badgeTotalChat.visibility = View.VISIBLE
                    binding.badgeTotalChat.text = if (count > 99) "+99" else count.toString()
                } else {
                    binding.badgeTotalChat.visibility = View.GONE
                }
            }
    }
    private fun selectTab(selectedLayout: LinearLayout) {
        tabs.forEachIndexed { index, tab ->

            // Aseguramos que el icono siempre sea BLANCO
            tab.icon.setColorFilter(Color.WHITE)

            // Aseguramos que el texto siempre sea BLANCO (por si acaso el XML tenía otro color)
            tab.text.setTextColor(Color.WHITE)

            if (tab.layout == selectedLayout) {
                // === SELECCIONADO ===

                // 1. Fondo marrón/rojizo (Según posición)
                when (index) {
                    0 -> tab.layout.setBackgroundResource(R.drawable.bg_tab_selected_left)
                    tabs.size - 1 -> tab.layout.setBackgroundResource(R.drawable.bg_tab_selected_right)
                    else -> tab.layout.setBackgroundResource(R.drawable.bg_tab_selected_center)
                }

                // 2. Animación (Subir y mostrar texto)
                if (tab.text.visibility != View.VISIBLE) {
                    tab.icon.animate().translationY(-8f).setDuration(200).start()

                    tab.text.visibility = View.VISIBLE
                    tab.text.alpha = 0f
                    tab.text.animate().alpha(1f).setDuration(200).start()
                }

            } else {
                // === NO SELECCIONADO ===

                // 1. Quitar fondo
                tab.layout.background = null

                // 2. Resetear animación
                tab.icon.animate().translationY(0f).setDuration(200).start()
                tab.text.visibility = View.GONE
            }
        }
    }
    private fun setupTabs() {
        tabs = listOf(
            TabItem(binding.tabAvatar, binding.imgAvatar, binding.txtAvatar),
            TabItem(binding.tabChat, binding.imgChat, binding.txtChat),
            TabItem(binding.tabPlay, binding.imgPlay, binding.txtPlay),
            TabItem(binding.tabFriends, binding.imgFriends, binding.txtFriends),
            TabItem(binding.tabStore, binding.imgStore, binding.txtStore)
        )
    }
}