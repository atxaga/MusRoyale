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

    data class TabItem(
        val layout: LinearLayout,
        val icon: ImageView,
        val text: TextView
    )

    private lateinit var tabs: List<TabItem>
    private var currentUserId: String? = null
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var chatNotificationsListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        currentUserId = prefs.getString("userRegistrado", null)

        // 1. Listeners de los nuevos botones del Header (Avatar arriba)
        binding.containerHeaderAvatar.setOnClickListener {
            mostrarSelectorAvatares()
        }

        // 2. Escucha de notificaciones de chat
        if (currentUserId != null) {
            escucharNotificacionesChat()
        }

        // 3. Setup de Tabs y Fragment inicial
        setupTabs()
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // 4. Listeners de Botones de Cabecera
        binding.btnLogout.setOnClickListener { logout() }
        binding.btnAddBalance.setOnClickListener {
            startActivity(Intent(this, CryptoPaymentActivity::class.java))
        }

        setupFooterListeners()
        cargarDatosUser()
    }

    override fun onResume() {
        super.onResume()
        cargarDatosUser()
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
                rv.adapter = AvatarAdapter(misAvatares, actual) { avatarSeleccionado ->
                    actualizarAvatarActual(userId, avatarSeleccionado)
                    bottomSheet.dismiss()
                }
            }
        bottomSheet.show()
    }

    private fun actualizarAvatarActual(userId: String, nuevoAvatar: String) {

        FirebaseFirestore.getInstance().collection("Users").document(userId)
            .update("avatarActual", nuevoAvatar)
            .addOnSuccessListener {
                // El SnapshotListener de cargarDatosUser se encargará de actualizar las imágenes
                Toast.makeText(this, "Avatarra aldatuta!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarDatosUser() {
        if (currentUserId == null) return

        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("Users").document(currentUserId!!)

        userListener?.remove()
        userListener = docRef.addSnapshotListener { document, error ->
            if (error != null) return@addSnapshotListener

            if (document != null && document.exists()) {
                binding.loadingOverlay.visibility = View.GONE

                // Actualizar Textos
                binding.txtUsername.text = document.getString("username") ?: "Usuario"
                binding.txtBalance.text = document.get("dinero")?.toString() ?: "0"

                // === CARGAR AVATAR ACTUAL (CORREGIDO) ===
                val avatarActualStr = document.getString("avatarActual") // Ej: "rey.png"
                val resId = getResIdFromName(this, avatarActualStr)

                if (resId != 0) {
                    binding.imgHeaderAvatar.setImageResource(resId)
                    binding.imgAvatar.setImageResource(resId)
                } else {
                    // Imagen por defecto si el ID es 0 o no existe
                    binding.imgHeaderAvatar.setImageResource(R.drawable.ic_avatar3)
                    binding.imgAvatar.setImageResource(R.drawable.ic_avatar3)
                }

                // Lógica de Admin
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

    private fun setupFooterListeners() {
        binding.tabAvatar.setOnClickListener {
            // Ahora que el avatar está arriba, este botón puede abrir el selector
            startActivity(Intent(this, EditProfileActivity::class.java))

        }

        binding.tabChat.setOnClickListener {
            startActivity(Intent(this, ChatSplitActivity::class.java))
        }

        binding.tabPlay.setOnClickListener {
            //selectTab(binding.tabPlay)
            loadFragment(HomeFragment())
            binding.header.visibility = View.VISIBLE
        }

        binding.tabFriends.setOnClickListener {
            //selectTab(binding.tabFriends)
            loadFragment(FriendsFragment())
            binding.header.visibility = View.GONE
        }

        binding.tabStore.setOnClickListener {
            //selectTab(binding.tabStore)
            loadFragment(StoreFragment())
            binding.header.visibility = View.GONE
        }
    }

    // --- RESTO DE FUNCIONES (Iguales pero mantenidas para consistencia) ---

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContainer, fragment)
            .commit()
    }

    private fun escucharNotificacionesChat() {
        val uid = currentUserId ?: return
        chatNotificationsListener = FirebaseFirestore.getInstance().collection("Chats")
            .whereEqualTo("idreceptor", uid)
            .whereEqualTo("leido", false)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                val count = snapshots?.size() ?: 0
                binding.badgeTotalChat.visibility = if (count > 0) View.VISIBLE else View.GONE
                binding.badgeTotalChat.text = if (count > 99) "+99" else count.toString()
            }
    }

    private fun selectTab(selectedLayout: LinearLayout) {
        tabs.forEachIndexed { index, tab ->
            // IMPORTANTE: Quitamos el filtro blanco para que el AVATAR se vea en COLOR
            if (tab.layout != binding.tabAvatar) {
                tab.icon.setColorFilter(Color.WHITE)
            } else {
                tab.icon.clearColorFilter()
            }

            tab.text.setTextColor(Color.WHITE)

            if (tab.layout == selectedLayout) {
                when (index) {
                    0 -> tab.layout.setBackgroundResource(R.drawable.bg_tab_selected_left)
                    tabs.size - 1 -> tab.layout.setBackgroundResource(R.drawable.bg_tab_selected_right)
                    else -> tab.layout.setBackgroundResource(R.drawable.bg_tab_selected_center)
                }
                if (tab.text.visibility != View.VISIBLE) {
                    tab.icon.animate().translationY(-8f).setDuration(200).start()
                    tab.text.visibility = View.VISIBLE
                    tab.text.alpha = 0f
                    tab.text.animate().alpha(1f).setDuration(200).start()
                }
            } else {
                tab.layout.background = null
                tab.icon.animate().translationY(0f).setDuration(200).start()
                tab.text.visibility = View.GONE
            }
        }
    }
    private fun getResIdFromName(context: Context, nameWithExtension: String?): Int {
        if (nameWithExtension == null) return 0
        // Limpiamos el ".png" si existe para que getIdentifier funcione
        val cleanName = nameWithExtension.replace(".png", "")
        return context.resources.getIdentifier(cleanName, "drawable", context.packageName)
    }
    private fun setupTabs() {
        tabs = listOf(
            //TabItem(binding.tabAvatar, binding.imgAvatar, binding.txtAvatar),
            //TabItem(binding.tabChat, binding.imgChat, binding.txtChat),
            //TabItem(binding.tabPlay, binding.imgPlay, binding.txtPlay),
            //TabItem(binding.tabFriends, binding.imgFriends, binding.txtFriends),
            //TabItem(binding.tabStore, binding.imgStore, binding.txtStore)
        )
    }

    fun logout(){
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