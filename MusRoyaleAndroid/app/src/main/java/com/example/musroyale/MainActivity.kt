package com.example.musroyale

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.database.FirebaseDatabase
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentUserId: String? = null
    private val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    private var rewardedAd: RewardedAd? = null
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var chatNotificationsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private val amigosListeners = mutableMapOf<String, com.google.firebase.database.ValueEventListener>()
    private val databaseRT = FirebaseDatabase.getInstance("https://musroyale-488aa-default-rtdb.europe-west1.firebasedatabase.app/")

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
        MobileAds.initialize(this) { status ->
            // Una vez inicializado, cargamos el primer anuncio
            cargarAnuncioRecompensa()
        }
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
        binding.btnAddOro.setOnClickListener { mostrarDialogoCompraOro() }

        if (currentUserId != null) escucharNotificacionesChat()
        cargarDatosUser()
        configurarSistemaPresencia(currentUserId.toString())
        iniciarObservadorAmigos()
    }
    private fun iniciarObservadorAmigos() {
        val uid = currentUserId ?: return

        // 1. Obtenemos la lista de amigos de Firestore
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
            .addOnSuccessListener { snapshot ->
                val listaAmigos = snapshot.get("amigos") as? List<String> ?: emptyList()

                for (amigoId in listaAmigos) {
                    escucharEstadoAmigo(amigoId)
                }
            }
    }

    private fun escucharEstadoAmigo(amigoId: String) {
        val ref = databaseRT.getReference("estado_usuarios/$amigoId")

        val listener = object : com.google.firebase.database.ValueEventListener {
            private var primeraVez = true // Para evitar que salte al abrir la app

            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val estado = snapshot.getValue(String::class.java) ?: "offline"

                if (!primeraVez && estado == "online") {
                    // El amigo se acaba de conectar, buscamos sus datos para el banner
                    obtenerDatosYMostrarBanner(amigoId)
                }
                primeraVez = false
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }

        ref.addValueEventListener(listener)
        amigosListeners[amigoId] = listener
    }

    private fun obtenerDatosYMostrarBanner(amigoId: String) {
        FirebaseFirestore.getInstance().collection("Users").document(amigoId).get()
            .addOnSuccessListener { doc ->
                val nombre = doc.getString("username") ?: "Lagun bat"
                val avatar = doc.getString("avatarActual")
                mostrarBannerTop(nombre, avatar)
            }
    }

    private fun mostrarBannerTop(nombre: String, avatarStr: String?) {
        val inflater = layoutInflater
        // Usamos binding.root para asegurarnos de que se añade a la base de la actividad
        val layout = inflater.inflate(R.layout.layout_notification_online, binding.root, false)

        layout.findViewById<TextView>(R.id.txtNotifyMessage).text = "$nombre konektatua!"
        val img = layout.findViewById<ImageView>(R.id.imgNotifyAvatar)
        val resId = getResIdFromName(this, avatarStr)
        img.setImageResource(if (resId != 0) resId else R.drawable.ic_avatar3)

        // Añadimos la vista al root
        binding.root.addView(layout)

        // Alineamos el banner arriba (opcional si el XML ya lo hace)
        layout.translationZ = 100f // Asegura que esté por encima de botones y tabs

        layout.translationY = -300f
        layout.animate()
            .translationY(100f) // Baja hasta 100px desde el tope
            .setDuration(600)
            .setInterpolator(android.view.animation.OvershootInterpolator())
            .withEndAction {
                layout.postDelayed({
                    layout.animate()
                        .translationY(-400f)
                        .alpha(0f)
                        .setDuration(500)
                        .withEndAction { binding.root.removeView(layout) }
                        .start()
                }, 3500)
            }
            .start()
    }
    private fun cargarAnuncioRecompensa() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
            }
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
        })
    }

    fun configurarSistemaPresencia(uid: String) {
        // 1. Referencia a Realtime Database (la base de datos rápida)
        val database = FirebaseDatabase.getInstance("https://musroyale-488aa-default-rtdb.europe-west1.firebasedatabase.app/")
        val miEstadoRef = database.getReference("estado_usuarios/$uid")

        // 2. Escuchar la conexión del sistema
        val connectedRef = database.getReference(".info/connected")

        connectedRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val conectado = snapshot.getValue(Boolean::class.java) ?: false

                if (conectado) {
                    // Si el servidor detecta que pierdo internet, me pone offline automáticamente
                    miEstadoRef.onDisconnect().setValue("offline")

                    // Ahora mismo estoy online
                    miEstadoRef.setValue("online")
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
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
            navegarAFuncion(binding.tabAvatar, binding.imgAvatar, "PERFIL")
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
            "PERFIL" -> EditProfileFragment()
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

                // 1. Datos básicos
                val username = document.getString("username") ?: "Usuario"
                binding.txtUsername.text = username
                binding.txtBalance.text = document.get("dinero")?.toString() ?: "0"
                binding.txtOro.text = document.get("oro")?.toString() ?: "0"

                // --- LÓGICA PREMIUM EN TIEMPO REAL ---
                val esPremium = document.getBoolean("premium") ?: false

                if (esPremium) {
                    binding.imgPremiumBadge.visibility = View.VISIBLE
                    binding.txtUsername.setTextColor(Color.parseColor("#FFD700"))

                    // CORRECCIÓN AQUÍ: Usamos ColorStateList.valueOf para el borde
                } else {
                    binding.imgPremiumBadge.visibility = View.GONE
                    binding.txtUsername.setTextColor(Color.WHITE)

                    // CORRECCIÓN AQUÍ: Borde transparente o gris normal
                }
                // -------------------------------------

                val avatarResId = getResIdFromName(this, document.getString("avatarActual"))
                binding.imgHeaderAvatar.setImageResource(if (avatarResId != 0) avatarResId else R.drawable.ic_avatar3)
                val userRol = document.get("rol") as? Long ?: 0

                if (userRol == 1L) {
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
    private fun mostrarDialogoCompraOro() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        // Inflamos el XML que modificamos con el botón de "Oro gratis"
        val view = layoutInflater.inflate(R.layout.dialog_store_oro_v2, null)
        dialog.setContentView(view)

        val container = view.findViewById<LinearLayout>(R.id.containerPacks)

        // --- NUEVA LÓGICA PARA EL ANUNCIO ---
        val btnFreeGold = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.btnFreeGold)
        btnFreeGold.addClickScaleAnimation() // Usamos tu extensión de animación

        btnFreeGold.setOnClickListener {
            if (rewardedAd != null) {
                rewardedAd?.show(this) { rewardItem ->
                    // Al terminar de ver el anuncio:
                    otorgarRecompensaAnuncio(250)
                    dialog.dismiss() // Cerramos el diálogo tras el premio
                }
            } else {
                Toast.makeText(this, "Iragarkia kargatzen... Saiatu berriro", Toast.LENGTH_SHORT).show()
                cargarAnuncioRecompensa() // Reintentar carga
            }
        }
        // -------------------------------------

        // Tu lógica actual de los packs pagados
        val packs = listOf(Pair(1000, 1.0), Pair(5000, 4.0), Pair(15000, 10.0), Pair(50000, 30.0))
        packs.forEach { pack ->
            val itemView = layoutInflater.inflate(R.layout.item_pack_oro_row, container, false)
            itemView.addClickScaleAnimation()
            itemView.findViewById<TextView>(R.id.txtCantidadOro).text = "${String.format("%,d", pack.first)} ORO"
            itemView.findViewById<TextView>(R.id.txtPrecioBtn).text = "${pack.second}€"
            itemView.setOnClickListener {
                procesarCompra(pack.first, pack.second)
                dialog.dismiss()
            }
            container.addView(itemView)
        }
        dialog.show()
    }
    private fun otorgarRecompensaAnuncio(cantidad: Int) {
        val uid = currentUserId ?: return
        val userRef = FirebaseFirestore.getInstance().collection("Users").document(uid)

        userRef.get().addOnSuccessListener { snapshot ->
            val oroActualString = snapshot.getString("oro") ?: "0"
            val oroActual = oroActualString.toIntOrNull() ?: 0
            val nuevoOro = oroActual + cantidad

            userRef.update("oro", nuevoOro.toString())
                .addOnSuccessListener {
                    Toast.makeText(this, "¡+250 Urre lortu duzu! 💰", Toast.LENGTH_LONG).show()
                    cargarAnuncioRecompensa() // Cargamos el siguiente para la próxima vez
                }
        }
    }
    private fun procesarCompra(cantidadOro: Int, costoDinero: Double) {
        val uid = currentUserId ?: return
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("Users").document(uid)

        binding.loadingOverlay.visibility = View.VISIBLE

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)

            // 1. Obtener Strings y convertirlos a decimales (Double)
            val dineroString = snapshot.getString("dinero") ?: "0.00"
            val oroString = snapshot.getString("oro") ?: "0"

            // Reemplazamos coma por punto por seguridad si el sistema está en español
            val dineroActual = dineroString.replace(",", ".").toDoubleOrNull() ?: 0.0
            val oroActual = oroString.toIntOrNull() ?: 0

            // 2. Verificar saldo
            if (dineroActual >= costoDinero) {
                val nuevoDinero = dineroActual - costoDinero
                val nuevoOro = oroActual + cantidadOro

                // 3. Guardar de nuevo como String con formato de 2 decimales
                // El "%.2f" asegura que se guarde algo como "14.99" y no "14.990000001"
                val nuevoDineroString = String.format("%.2f", nuevoDinero).replace(",", ".")

                transaction.update(userRef, "dinero", nuevoDineroString)
                transaction.update(userRef, "oro", nuevoOro.toString())

                true
            } else {
                throw Exception("SALDO_INSUFICIENTE")
            }
        }.addOnSuccessListener {
            binding.loadingOverlay.visibility = View.GONE
            Toast.makeText(this, "¡Compra realizada con éxito!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            binding.loadingOverlay.visibility = View.GONE
            if (e.message == "SALDO_INSUFICIENTE") {
                Toast.makeText(this, "No tienes dinero suficiente", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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

        // Eliminar todos los listeners de amigos
        amigosListeners.forEach { (id, listener) ->
            databaseRT.getReference("estado_usuarios/$id").removeEventListener(listener)
        }
    }
}
private fun View.addClickScaleAnimation() {
    this.setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
        }
        false // Importante: false para que el OnClickListener de arriba siga funcionando
    }
}