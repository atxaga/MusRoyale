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

class StoreFragment : Fragment() {

    private var _binding: FragmentStoreBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private val storeAdapter by lazy { StoreProductAdapter(StoreProductDiffCallback) { showPurchaseFeedback(it) } }
    private val catalog by lazy { createCatalog() }

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

        // 2. Configurar el clic para abrir el LootboxFragment
        binding.btnOpenLootboxView.setOnClickListener {
            val lootboxFragment = LootboxFragment()

            parentFragmentManager.beginTransaction()
                .replace(R.id.mainContainer, lootboxFragment)
                .addToBackStack(null) // Permite volver atrás a la tienda
                .commit()
        }
        // 1. Configurar RecyclerView de Productos

        storeAdapter.submitList(catalog)

        // 2. Click Ruleta (Dinero)
        binding.btnSpinWheel.setOnClickListener {
            comprobarSaldoYEjecutar(0.30) { ejecutarGiroRuleta() }
        }


        // 3. Click Tragaperras (Skins)
        binding.btnSpinSkins.setOnClickListener {
            comprobarSaldoYEjecutar(0.50) { ejecutarGiroTragaperras() }
        }

        // 4. Buscador
        binding.etStoreSearch.doAfterTextChanged { text ->
            val query = text?.toString()?.trim()?.lowercase().orEmpty()
            val filtered = if (query.isEmpty()) catalog else catalog.filter {
                it.name.lowercase().contains(query) || it.description.lowercase().contains(query)
            }
            storeAdapter.submitList(filtered)
        }
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

    private inner class StoreProductAdapter(
        diffCallback: DiffUtil.ItemCallback<StoreProduct>,
        private val onBuyClick: (StoreProduct) -> Unit
    ) : ListAdapter<StoreProduct, StoreProductViewHolder>(diffCallback) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            StoreProductViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_store_product, parent, false), onBuyClick)
        override fun onBindViewHolder(holder: StoreProductViewHolder, position: Int) = holder.bind(getItem(position))
    }

    private class StoreProductViewHolder(view: View, val onBuyClick: (StoreProduct) -> Unit) : RecyclerView.ViewHolder(view) {
        fun bind(product: StoreProduct) {
            itemView.findViewById<TextView>(R.id.tvProductName).text = product.name
            itemView.findViewById<TextView>(R.id.tvProductPrice).text = product.priceLabel
            itemView.findViewById<ImageView>(R.id.ivProductImage).setImageResource(product.imageRes)
            itemView.findViewById<MaterialButton>(R.id.btnBuy).setOnClickListener { onBuyClick(product) }
        }
    }

    private object StoreProductDiffCallback : DiffUtil.ItemCallback<StoreProduct>() {
        override fun areItemsTheSame(old: StoreProduct, new: StoreProduct) = old.id == new.id
        override fun areContentsTheSame(old: StoreProduct, new: StoreProduct) = old == new
    }
}