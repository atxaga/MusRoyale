package com.example.musroyale

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import android.app.AlertDialog // Asegúrate de importar esto
import android.graphics.Color // Para los colores
import android.graphics.Typeface // Para el tipo de letra
import android.graphics.drawable.ColorDrawable

class StoreFragment : Fragment() {

    private var _binding: FragmentStoreBinding? = null
    private val binding get() = _binding!!
    private var currentUserId: String? = null


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

        // 1. Configurar RecyclerView
        binding.rvStoreProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = storeAdapter
        }
        storeAdapter.submitList(catalog)


        // 2. Lógica de búsqueda
        binding.etStoreSearch.doAfterTextChanged { text ->
            val query = text?.toString()?.trim()?.lowercase().orEmpty()
            val filtered = if (query.isEmpty()) catalog else catalog.filter {
                it.name.lowercase().contains(query) || it.description.lowercase().contains(query)
            }
            storeAdapter.submitList(filtered)
        }

        // 3. Lógica de la Ruleta
        binding.btnSpinWheel.setOnClickListener {
            val prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val userId = prefs.getString("userRegistrado", null)

            if (userId != null) {
                val db = FirebaseFirestore.getInstance()
                // Consultamos el saldo rápidamente antes de empezar
                db.collection("Users").document(userId).get().addOnSuccessListener { snapshot ->
                    val saldoActual = snapshot.getString("dinero")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

                    if (saldoActual >= 0.30) {
                        // TIENE DINERO: Empezamos la animación
                        ejecutarGiroRuleta()
                    } else {
                        // NO TIENE DINERO: Alerta inmediata
                        mostrarAlertaElegante(-1.0, "Saldo insuficiente (0.30€)", "#E74C3C")
                    }
                }
            }
        }
    }

    private fun ejecutarGiroRuleta() {
        // Bloqueamos el botón solo durante el giro para que no se raye la animación
        binding.btnSpinWheel.isEnabled = false

        val vueltas = 6
        val anguloAzar = (0..360).random().toFloat()
        val gradosTotales = (vueltas * 360 + anguloAzar)

        binding.ivWheel.animate()
            .rotationBy(gradosTotales)
            .setDuration(3500)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                determinarPremio(binding.ivWheel.rotation)

                // COMENTADO PARA PRUEBAS: Aquí iría la lógica de bloqueo diario
                // binding.btnSpinWheel.text = "VUELVE MAÑANA"
                // binding.btnSpinWheel.isEnabled = false

                // RE-ACTIVAR PARA PRUEBAS (Permite girar de nuevo tras 1 segundo)
                binding.btnSpinWheel.postDelayed({
                    binding.btnSpinWheel.isEnabled = true
                }, 1000)
            }
            .start()
    }

    private fun determinarPremio(gradosFinales: Float) {
        val anguloNormalizado = (gradosFinales % 360 + 360) % 360
        val sectorApuntado = (360 - anguloNormalizado)

        var premioEuros = 0.0

        when (sectorApuntado) {
            in 358.0..360.0 -> premioEuros = 20.00
            in 347.5..357.0 -> premioEuros = 0.50
            in 247.5..292.5 -> premioEuros = 0.20
            in 67.5..112.5 -> premioEuros = 0.10
            else -> premioEuros = 0.0
        }

        // Llamamos a la función que resta 0.30 y suma el premio
        actualizarDineroTrasGiro(premioEuros)
    }
    private fun actualizarDineroTrasGiro(cantidadGanada: Double) {
        val costeGiro = 0.30
        val prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("userRegistrado", null) ?: return

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("Users").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)

            // 1. Obtener saldo actual
            val saldoActualString = snapshot.getString("dinero") ?: "0.00"
            val saldoActualDouble = saldoActualString.replace(",", ".").toDoubleOrNull() ?: 0.0

            // 2. Verificar si tiene saldo suficiente para pagar el giro
            if (saldoActualDouble < costeGiro) {
                throw Exception("Ez duzu dirurik") // Esto cancela la transacción
            }

            // 3. Calcular nuevo saldo: (Saldo - 0.30) + Premio
            val nuevoSaldoDouble = saldoActualDouble - costeGiro + cantidadGanada
            val nuevoSaldoString = String.format("%.2f", nuevoSaldoDouble).replace(",", ".")

            // 4. Actualizar en Firebase
            transaction.update(userRef, "dinero", nuevoSaldoString)

            nuevoSaldoString
        }.addOnSuccessListener { nuevoTotal ->
            // Mostrar la alerta elegante que creamos antes
            if (cantidadGanada > 0) {
                mostrarAlertaPremio("Irabazi duzu ${String.format("%.2f", cantidadGanada)}€", cantidadGanada)
            } else {
                mostrarAlertaPremio("Zorte txarra!", 0.0)
            }
        }.addOnFailureListener { e ->
            if (e.message == "Ez duzu dirurik") {
                Toast.makeText(requireContext(), "Gutzienez 0.30€ behar dituzu", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            // Reactivamos el botón para que pueda intentar de nuevo si recarga saldo
            binding.btnSpinWheel.isEnabled = true
        }
    }
    private fun actualizarDineroEnBaseDeDatos(cantidadGanada: Double) {
        // 1. Usamos "UserPrefs" que es el que me confirmas que funciona
        val prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("userRegistrado", null)

        if (userId == null) {
            Toast.makeText(requireContext(), "Error: No se encontró el ID del usuario", Toast.LENGTH_SHORT).show()
            binding.btnSpinWheel.isEnabled = true
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("Users").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)

            // Si el documento existe, leemos el dinero. Si no, empezamos en 0.0
            val saldoActualDouble = if (snapshot.exists()) {
                val saldoActualString = snapshot.getString("dinero") ?: "0"
                // Reemplazamos coma por punto por si acaso y convertimos a número
                saldoActualString.replace(",", ".").toDoubleOrNull() ?: 0.0
            } else {
                0.0
            }

            val nuevoSaldoDouble = saldoActualDouble + cantidadGanada

            // Formateamos a 2 decimales y usamos punto como separador
            val nuevoSaldoString = String.format("%.2f", nuevoSaldoDouble).replace(",", ".")

            // Si el usuario no tenía documento en la colección "users", lo creamos (set)
            // Si ya existía, solo actualizamos el campo "dinero" (update)
            if (!snapshot.exists()) {
                val datosNuevos = hashMapOf("dinero" to nuevoSaldoString)
                transaction.set(userRef, datosNuevos)
            } else {
                transaction.update(userRef, "dinero", nuevoSaldoString)
            }

            nuevoSaldoString // Retornamos el valor para el SuccessListener
        }.addOnSuccessListener { nuevoTotal ->
            Toast.makeText(requireContext(), "¡Ingreso de $cantidadGanada€ realizado! Saldo total: $nuevoTotal €", Toast.LENGTH_LONG).show()

            // Como estamos en modo prueba, reactivamos el botón
            binding.btnSpinWheel.text = "GIRAR DE NUEVO"
            binding.btnSpinWheel.isEnabled = true
            binding.btnSpinWheel.alpha = 1.0f

        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error en Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.btnSpinWheel.isEnabled = true
            binding.btnSpinWheel.alpha = 1.0f
        }
    }
    private fun mostrarAlertaElegante(cantidad: Double, subtexto: String, colorHex: String) {
        // 1. Inflar el diseño personalizado que creamos en el XML
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_prize_alert, null)

        // 2. Referenciar los componentes del XML
        val tvAmount: TextView = dialogView.findViewById(R.id.tvDialogMessage)
        val tvSubTitle: TextView = dialogView.findViewById(R.id.tvDialogTitle)
        val btnAccept: TextView = dialogView.findViewById(R.id.btnDialogAccept)

        // 3. Lógica de visualización según el resultado
        when {
            cantidad > 0 -> {
                // Ganó algo (ej. 1.00€ o 5.00€)
                tvAmount.text = String.format("%.2f €", cantidad)
                tvAmount.setTextColor(Color.parseColor(colorHex)) // Color verde pasado por parámetro
                tvSubTitle.text = subtexto
            }
            cantidad == 0.0 -> {
                // No ganó nada (0.00€), pero pagó el giro
                tvAmount.text = "0.00 €"
                tvAmount.setTextColor(Color.parseColor("#BDC3C7")) // Gris elegante
                tvSubTitle.text = subtexto
            }
            else -> {
                // Caso de error o saldo insuficiente (cantidad -1.0)
                tvAmount.text = "¡Ops!"
                tvAmount.setTextColor(Color.parseColor("#E67E22")) // Naranja sutil
                tvSubTitle.text = subtexto
            }
        }

        // 4. Crear y configurar el AlertDialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false) // Obliga al usuario a pulsar el botón para cerrar
            .create()

        // 5. IMPORTANTE: Hace que el fondo del diálogo sea transparente
        // para que se vean las esquinas redondeadas de tu CardView
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 6. Configurar el botón de cierre
        btnAccept.setOnClickListener {
            dialog.dismiss()

            // Al cerrar la alerta, nos aseguramos de que el botón de la ruleta
            // esté listo para otro giro (solo si tiene saldo, esto lo maneja el click principal)
            binding.btnSpinWheel.isEnabled = true
            binding.btnSpinWheel.alpha = 1.0f
        }

        dialog.show()
    }
    private fun mostrarAlertaPremio(mensaje: String, cantidad: Double) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_prize_alert, null)

        val tvAmount: TextView = dialogView.findViewById(R.id.tvDialogMessage)
        val tvSubTitle: TextView = dialogView.findViewById(R.id.tvDialogTitle)
        val btnAccept: TextView = dialogView.findViewById(R.id.btnDialogAccept)

        // Si ganó, mostramos el número. Si no, un mensaje suave.
        if (cantidad > 0.0) {
            tvAmount.text = String.format("%.2f €", cantidad)
            tvSubTitle.text = "Zorionak!"
            tvAmount.setTextColor(Color.parseColor("#27AE60")) // Un verde elegante
        } else {
            tvAmount.text = "—"
            tvSubTitle.text = "Zorte txarra!"
            tvAmount.setTextColor(Color.parseColor("#BDC3C7")) // Gris suave
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Fondo transparente para que se vea el redondeado de la CardView
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnAccept.setOnClickListener {
            dialog.dismiss()
            binding.btnSpinWheel.isEnabled = true
        }

        dialog.show()
    }

    private fun showPurchaseFeedback(product: StoreProduct) {
        Toast.makeText(requireContext(), "Comprado: ${product.name}", Toast.LENGTH_SHORT).show()
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

    // --- Clases del Adapter ---
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