package com.example.musroyale

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.musroyale.databinding.ActivityMainBinding
import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentUserId: String? = null
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var chatNotificationsListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.selectionIndicator.bringToFront()
        binding.footerIcons.bringToFront() // Suponiendo que le pusiste este ID al LinearLayout
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        currentUserId = prefs.getString("userRegistrado", null)

        // 1. Inicializar Tabs (Movimiento del círculo y clics)
        setupTabs()

        // 2. Cargar Fragmento inicial (Home)
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // 3. Listeners de Cabecera
        binding.containerHeaderAvatar.setOnClickListener { mostrarSelectorAvatares() }
        binding.btnLogout.setOnClickListener { logout() }
        binding.btnAddBalance.setOnClickListener {
            startActivity(Intent(this, CryptoPaymentActivity::class.java))
        }

        if (currentUserId != null) escucharNotificacionesChat()
        cargarDatosUser()
    }

    private fun setupTabs() {
        binding.selectionIndicator.translationZ = 10f
        binding.footer.translationZ = 5f

        // Pestañas de navegación central
        binding.tabPlay.setOnClickListener {
            navegarAFuncion(binding.tabPlay, binding.imgPlay, "HOME")
        }

        binding.tabFriends.setOnClickListener {
            navegarAFuncion(binding.tabFriends, binding.imgFriends, "FRIENDS")
        }

        binding.tabStore.setOnClickListener {
            navegarAFuncion(binding.tabStore, binding.imgStore, "STORE")
        }

        // Pestañas laterales (Sin movimiento de círculo)
        binding.tabAvatar.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.tabChat.setOnClickListener {
            startActivity(Intent(this, ChatSplitActivity::class.java))
        }

        // Posición inicial
        binding.footer.post {
            updateTabUI(binding.tabPlay, binding.imgPlay, animate = false)
        }
    }

    // Nueva función de ayuda para evitar crear fragmentos por duplicado
    private fun navegarAFuncion(layout: View, icon: ImageView, destino: String) {
        updateTabUI(layout, icon, animate = true)

        val fragment = when (destino) {
            "HOME" -> HomeFragment()
            "FRIENDS" -> FriendsFragment()
            "STORE" -> StoreFragment()
            else -> HomeFragment()
        }

        loadFragment(fragment)
        binding.header.visibility = if (destino == "HOME") View.VISIBLE else View.GONE
    }

    private fun updateTabUI(targetLayout: View, targetIcon: ImageView, animate: Boolean) {
        // 1. Mover el círculo indicador
        val targetX = targetLayout.left + (targetLayout.width / 2f) - (binding.selectionIndicator.width / 2f) + binding.footer.left

        if (animate) {
            binding.selectionIndicator.animate()
                .x(targetX)
                .setDuration(400)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                .start()
        } else {
            binding.selectionIndicator.x = targetX
        }

        // 2. Animación de los iconos (Color y Tamaño)
        val tabs = listOf(
            binding.imgAvatar,
            binding.imgChat,
            binding.imgPlay,
            binding.imgFriends,
            binding.imgStore
        )

        tabs.forEach { icon ->
            if (icon == targetIcon) {
                // ICONO SELECCIONADO: Negro y se agranda
                icon.setColorFilter(Color.BLACK)
                icon.animate()
                    .scaleX(1.4f) // Crece un 40%
                    .scaleY(1.4f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.AnticipateOvershootInterpolator())
                    .start()
            } else {
                // ICONOS NO SELECCIONADOS: Blancos y tamaño normal
                icon.setColorFilter(Color.WHITE)
                icon.animate()
                    .scaleX(1.0f) // Vuelve a su tamaño original
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()
            }
        }
    }

    // --- Funciones de utilidad y Firebase ---

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContainer, fragment) // <--- ESTE ID DEBE SER IGUAL AL DEL XML
            .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
    }

    private fun cargarDatosUser() {
        if (currentUserId == null) return
        val docRef = FirebaseFirestore.getInstance().collection("Users").document(currentUserId!!)
        userListener = docRef.addSnapshotListener { document, _ ->
            if (document != null && document.exists()) {
                binding.loadingOverlay.visibility = View.GONE
                binding.txtUsername.text = document.getString("username") ?: "Usuario"
                binding.txtBalance.text = document.get("dinero")?.toString() ?: "0"

                val avatarResId = getResIdFromName(this, document.getString("avatarActual"))
                binding.imgHeaderAvatar.setImageResource(if (avatarResId != 0) avatarResId else R.drawable.ic_avatar3)

                binding.btnAdminPanel.visibility = if (currentUserId == "kHjrbXVjxZQzRHRvvqf7") View.VISIBLE else View.GONE
                if (currentUserId == "kHjrbXVjxZQzRHRvvqf7") {
                    binding.btnAdminPanel.setOnClickListener { startActivity(Intent(this, AdminActivity::class.java)) }
                }
            }
        }
    }

    private fun getResIdFromName(context: Context, name: String?): Int {
        if (name == null) return 0
        return context.resources.getIdentifier(name.replace(".png", ""), "drawable", context.packageName)
    }

    private fun escucharNotificacionesChat() {
        val uid = currentUserId ?: return
        val badgeChat = findViewById<TextView>(R.id.badgeChat)

        chatNotificationsListener = FirebaseFirestore.getInstance().collection("Chats")
            .whereEqualTo("idreceptor", uid)
            .whereEqualTo("leido", false)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                if (snapshots != null && !snapshots.isEmpty) {
                    // Hay mensajes no leídos
                    val numMensajes = snapshots.size()
                    badgeChat.text = if (numMensajes > 9) "+9" else numMensajes.toString()
                    badgeChat.visibility = View.VISIBLE
                } else {
                    // No hay mensajes nuevos
                    badgeChat.visibility = View.GONE
                }
            }
    }

    private fun mostrarSelectorAvatares() {
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_avatar_selector, null)
        bottomSheet.setContentView(view)
        val rv = view.findViewById<RecyclerView>(R.id.rvAvatarList)
        val userId = currentUserId ?: return

        FirebaseFirestore.getInstance().collection("Users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                val misAvatares = snapshot.get("avatares") as? List<String> ?: listOf("avatar_default")
                val actual = snapshot.getString("avatarActual") ?: "avatar_default"
                rv.layoutManager = GridLayoutManager(this, 3)
                rv.adapter = AvatarAdapter(misAvatares, actual) { avatar ->
                    FirebaseFirestore.getInstance().collection("Users").document(userId).update("avatarActual", avatar)
                    bottomSheet.dismiss()
                }
            }
        bottomSheet.show()
    }

    fun logout() {
        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().remove("userRegistrado").apply()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        userListener?.remove()
        chatNotificationsListener?.remove()
    }
}