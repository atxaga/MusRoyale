package com.example.musroyale

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musroyale.databinding.FragmentStoreBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

import android.widget.LinearLayout

class StoreFragment : Fragment() {

    private var _binding: FragmentStoreBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private val catalog by lazy { createCatalog() }
    private val currentUserId: String? by lazy {
        requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("userRegistrado", null)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val shake = ObjectAnimator.ofFloat(binding.ivBoxIcon, "translationY", 0f, -15f, 0f)
        shake.duration = 1200 // Un poco más de un segundo por ciclo
        shake.repeatCount = ObjectAnimator.INFINITE // No para nunca
        shake.start()
        cargarAvataresTienda(view)
        // 2. Configurar el clic para abrir el LootboxFragment
        binding.btnOpenLootboxView.setOnClickListener {
            val lootboxFragment = LootboxFragment()

            parentFragmentManager.beginTransaction()
                .replace(R.id.mainContainer, lootboxFragment)
                .addToBackStack(null) // Permite volver atrás a la tienda
                .commit()
        }
        // 1. Configurar RecyclerView de Productos


        // 2. Click Ruleta (Dinero)
        binding.btnSpinWheel.setOnClickListener {
            comprobarSaldoYEjecutar(0.30) { ejecutarGiroRuleta() }
        }


        // 3. Click Tragaperras (Skins)
        binding.btnSpinSkins.setOnClickListener {
            comprobarSaldoYEjecutar(0.50) { ejecutarGiroTragaperras() }
        }

        // 4. Buscador
        /*binding.etStoreSearch.doAfterTextChanged { text ->
            val query = text?.toString()?.trim()?.lowercase().orEmpty()
            val filtered = if (query.isEmpty()) catalog else catalog.filter {
                it.name.lowercase().contains(query) || it.description.lowercase().contains(query)
            }
            storeAdapter.submitList(filtered)
        }*/
    }

    // --- LÓGICA DE DINERO (FIREBASE) ---

    private fun comprobarSaldoYEjecutar(coste: Double, accion: () -> Unit) {
        val prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("userRegistrado", null) ?: return

        db.collection("Users").document(userId).get().addOnSuccessListener { snapshot ->
            val saldoActual = snapshot.getString("dinero")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
            if (saldoActual >= coste) {
                accion()
            } else {
                mostrarAlertaElegante(-1.0, "Saldo insuficiente (${String.format("%.2f", coste)}€)", "#E74C3C")
            }
        }
    }

    private fun descontarSaldo(coste: Double, premio: Double = 0.0) {
        val prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("userRegistrado", null) ?: return
        val userRef = db.collection("Users").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val saldoActual = snapshot.getString("dinero")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
            val nuevoSaldo = saldoActual - coste + premio
            val nuevoSaldoStr = String.format("%.2f", nuevoSaldo).replace(",", ".")
            transaction.update(userRef, "dinero", nuevoSaldoStr)
        }
    }
    private fun guardarAvatarGanado(avatarResId: Int) {
        val prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("userRegistrado", null) ?: return

        // Obtenemos el nombre (ej: "ava1") y le sumamos ".png"
        val nombreAvatar = resources.getResourceEntryName(avatarResId) + ".png"

        val userRef = db.collection("Users").document(userId)

        userRef.update("avatares", com.google.firebase.firestore.FieldValue.arrayUnion(nombreAvatar))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Bildumara gehituta!", Toast.LENGTH_SHORT).show()
            }
    }
    // --- LÓGICA TRAGAPERRAS (SLOT MACHINE) ---

    private fun ejecutarGiroTragaperras() {
        binding.btnSpinSkins.isEnabled = false
        descontarSaldo(0.50)

        val animacion = AnimationUtils.loadAnimation(requireContext(), R.anim.slot_spin)
        val misAvatares = listOf(R.drawable.ava1, R.drawable.ava2, R.drawable.ava3, R.drawable.ava4, R.drawable.ava5)

        // Iniciar visual solo para 2 slots
        binding.slot1.startAnimation(animacion)
        binding.slot2.startAnimation(animacion)

        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (binding.slot1.animation != null) binding.slot1.setImageResource(misAvatares.random())
                if (binding.slot2.animation != null) binding.slot2.setImageResource(misAvatares.random())
                handler.postDelayed(this, 100)
            }
        }
        handler.post(runnable)

        val res1 = misAvatares.random()
        val res2 = misAvatares.random()

        binding.slot1.postDelayed({ binding.slot1.clearAnimation(); binding.slot1.setImageResource(res1) }, 1500)
        binding.slot2.postDelayed({
            binding.slot2.clearAnimation()
            binding.slot2.setImageResource(res2)
            handler.removeCallbacks(runnable)
            binding.btnSpinSkins.isEnabled = true

            // AHORA SOLO COMPARA DOS
            if (res1 == res2) {
                mostrarNotificacionPremio(res1)
            } else {
                Toast.makeText(requireContext(), "Saiatu berriro!", Toast.LENGTH_SHORT).show()
            }
        }, 2200) // He bajado el tiempo a 2.2s para que sea más rápido al ser solo dos
    }

    private fun mostrarNotificacionPremio(avatarResId: Int) {
        // Inflamos el NUEVO layout win_alert
        val dialogView = layoutInflater.inflate(R.layout.dialog_win_alert, null)

        val ivAvatar: ImageView = dialogView.findViewById(R.id.ivAvatarWon)
        val btnAccept: TextView = dialogView.findViewById(R.id.btnWinAccept)

        // Ponemos la imagen que ha ganado
        ivAvatar.setImageResource(avatarResId)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Fondo transparente para que el redondeo de la CardView se vea bien
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // --- ANIMACIÓN DE ENTRADA ---
        ivAvatar.scaleX = 0f
        ivAvatar.scaleY = 0f

        dialog.setOnShowListener {
            ivAvatar.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(600)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    ivAvatar.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                }
                .start()
        }

        btnAccept.setOnClickListener {
            // LLAMADA AQUÍ: Guardamos el avatar en la lista del usuario antes de cerrar
            guardarAvatarGanado(avatarResId)

            dialog.dismiss()
        }

        dialog.show()
    }
    // --- LÓGICA RULETA (WHEEL) ---

    private fun ejecutarGiroRuleta() {
        binding.btnSpinWheel.isEnabled = false
        val gradosTotales = (6 * 360 + (0..360).random()).toFloat()

        binding.ivWheel.animate()
            .rotationBy(gradosTotales)
            .setDuration(3500)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                determinarPremioRuleta(binding.ivWheel.rotation)
                binding.btnSpinWheel.isEnabled = true
            }.start()
    }

    private fun determinarPremioRuleta(gradosFinales: Float) {
        val angulo = (360 - (gradosFinales % 360 + 360) % 360)
        val premio = when (angulo) {
            in 358.0..360.0 -> 20.0; in 347.5..357.0 -> 1.0
            in 247.5..292.5 -> 0.5; in 67.5..112.5 -> 0.3
            else -> 0.0
        }
        descontarSaldo(0.30, premio)
        mostrarAlertaPremio(if (premio > 0) "Irabazi duzu!" else "Zorte txarra!", premio)
    }

    // --- DIÁLOGOS Y AUXILIARES ---

    private fun mostrarAlertaElegante(cantidad: Double, subtexto: String, colorHex: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_prize_alert, null)
        val tvAmount: TextView = dialogView.findViewById(R.id.tvDialogMessage)
        val tvSubTitle: TextView = dialogView.findViewById(R.id.tvDialogTitle)
        val btnAccept: TextView = dialogView.findViewById(R.id.btnDialogAccept)

        tvAmount.text = if (cantidad >= 0) String.format("%.2f €", cantidad) else "¡Ops!"
        tvAmount.setTextColor(Color.parseColor(colorHex))
        tvSubTitle.text = subtexto

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).setCancelable(false).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        btnAccept.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun mostrarAlertaPremio(mensaje: String, cantidad: Double) {
        mostrarAlertaElegante(cantidad, mensaje, if (cantidad > 0) "#27AE60" else "#BDC3C7")
    }

    private fun showPurchaseFeedback(product: StoreProduct) {
        Toast.makeText(requireContext(), "Erosita: ${product.name}", Toast.LENGTH_SHORT).show()
    }

    private fun createCatalog(): List<StoreProduct> = listOf(
        StoreProduct("1", "Bolsa de Monedas", "500 monedas", "2.99€", android.R.drawable.ic_menu_gallery),
        StoreProduct("2", "Pack de Gemas", "50 gemas reales", "4.99€", android.R.drawable.ic_menu_send),
        StoreProduct("3", "Baraja Premium", "Estilo vintage", "1.99€", android.R.drawable.ic_menu_view)
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- CLASES DEL ADAPTER ---
    data class StoreProduct(val id: String, val name: String, val description: String, val priceLabel: String, @DrawableRes val imageRes: Int)

    private fun cargarAvataresTienda(view: View) {
        val gridContainer = view.findViewById<android.widget.GridLayout>(R.id.containerAvataresTienda) ?: return
        gridContainer.removeAllViews()

        val listaAvatares = listOf(
            Pair("ava1", 1500),
            Pair("ava2", 3000),
            Pair("ava3", 10000),
            Pair("ava4", 5000) // Añadimos uno más para que sean 4 (2x2)
        )

        listaAvatares.forEach { data ->
            val item = layoutInflater.inflate(R.layout.item_store_product, gridContainer, false)

            item.addClickScaleAnimation()

            val img = item.findViewById<ImageView>(R.id.imgAvatar)
            val txtPrecio = item.findViewById<TextView>(R.id.txtPrecioAvatar)
            val txtNombre = item.findViewById<TextView>(R.id.txtNombreAvatar)

            val resId = resources.getIdentifier(data.first, "drawable", requireContext().packageName)
            img.setImageResource(if(resId != 0) resId else R.drawable.ic_avatar3)

            txtNombre.text = data.first.replace("avatar_", "").uppercase()
            txtPrecio.text = data.second.toString()

            item.setOnClickListener {
                confirmarCompraAvatar(data.first, data.second)
            }

            gridContainer.addView(item)
        }
    }

    private fun confirmarCompraAvatar(nombreAvatar: String, precioOro: Int) {
        val uid = currentUserId ?: return
        val userRef = db.collection("Users").document(uid)

        // Mostrar loading si tienes uno en el fragment o un Toast de "Procesando"
        Toast.makeText(requireContext(), "Erosketa prozesatzen...", Toast.LENGTH_SHORT).show()

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)

            // IMPORTANTE: Leemos como String porque así lo tienes en tu DB
            val oroString = snapshot.getString("oro") ?: "0"
            val oroActual = oroString.toIntOrNull() ?: 0

            val misAvatares = snapshot.get("avatares") as? MutableList<String> ?: mutableListOf()

            // Comprobamos si ya tiene el avatar (con .png o sin él según lo guardes)
            val nombreConExtension = "$nombreAvatar.png"
            if (misAvatares.contains(nombreConExtension)) {
                throw Exception("YA_LO_TIENES")
            }

            if (oroActual >= precioOro) {
                val nuevoOro = oroActual - precioOro
                misAvatares.add(nombreConExtension)

                transaction.update(userRef, "oro", nuevoOro.toString())
                transaction.update(userRef, "avatares", misAvatares)
                true
            } else {
                throw Exception("ORO_INSUFICIENTE")
            }
        }.addOnSuccessListener {
            Toast.makeText(requireContext(), "¡Erosketa eginda! 🏆", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            val msg = when(e.message) {
                "YA_LO_TIENES" -> "Dagoeneko baduzu avatar hau!"
                "ORO_INSUFICIENTE" -> "Ez duzu nahikoa urre!"
                else -> "Errorea: ${e.message}"
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }


// Extension function para la animación (asegúrate de que esté fuera de la clase)




    private object StoreProductDiffCallback : DiffUtil.ItemCallback<StoreProduct>() {
        override fun areItemsTheSame(old: StoreProduct, new: StoreProduct) = old.id == new.id
        override fun areContentsTheSame(old: StoreProduct, new: StoreProduct) = old == new
    }
}
private fun View.addClickScaleAnimation() {
    this.setOnTouchListener { v, event ->
        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
            }
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
        }
        false
    }
}