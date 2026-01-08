package com.example.musroyale

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
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


        tabs = listOf(
            TabItem(binding.tabAvatar, binding.imgAvatar, binding.txtAvatar),
            TabItem(binding.tabChat, binding.imgChat, binding.txtChat),
            TabItem(binding.tabPlay, binding.imgPlay, binding.txtPlay),
            TabItem(binding.tabFriends, binding.imgFriends, binding.txtFriends),
            TabItem(binding.tabStore, binding.imgStore, binding.txtStore)
        )

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            selectTab(binding.tabPlay)
        }
        binding.btnLogout.setOnClickListener { logout() }

        setupFooterListeners()
        cargarDatosUser()
        binding.btnAddBalance.setOnClickListener {
            var intent = Intent(this, AddBalanceActivity::class.java)
            startActivity(intent)  }
    }

    fun logout(){
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        prefs.edit { remove("userRegistrado") }
        currentUserId = null
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun cargarDatosUser() {
        if (currentUserId != null) {

            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("Users").document(currentUserId!!)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        binding.loadingOverlay.visibility = View.GONE

                        val username = document.getString(  "username") ?: "Usuario"
                        val balance = document.getString("dinero") ?: "0"
                        binding.txtUsername.text = username
                        binding.txtBalance.text = balance
                    } else {
                        binding.txtUsername.text = getString(R.string.default_user)
                    }
                }
                .addOnFailureListener {
                    binding.txtUsername.text = getString(R.string.default_user)
                }
        }

    }

    private fun setupFooterListeners() {
        binding.tabAvatar.setOnClickListener {
            selectTab(binding.tabAvatar)
            loadFragment(EditProfileFragment())
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
}