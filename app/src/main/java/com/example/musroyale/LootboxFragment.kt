package com.example.musroyale

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.musroyale.databinding.FragmentLootboxBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Random

import android.widget.TextView
import android.widget.Button


class LootboxFragment : Fragment() {

    private var _binding: FragmentLootboxBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var currentUserId: String? = null

    // Lista de avatares disponibles (sin ".png")
    private val allAvatars = listOf("ava1", "ava2", "ava3", "ava4", "ava5", "ava_default")

    // Representación de los premios y sus pesos de probabilidad
    private val prizes = mutableListOf<Prize>()

    // Clase para representar un premio
    data class Prize(val type: PrizeType, val value: Any, val drawableResId: Int, val weight: Int)

    enum class PrizeType {
        AVATAR, MONEY
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLootboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        currentUserId = prefs.getString("userRegistrado", null)
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        setupPrizes() // Configura la lista de premios
        setupClickListeners()
        generateCarouselItems() // Genera los items iniciales en el carrusel
    }

    private fun setupPrizes() {
        // Avatares (50% de probabilidad total)
        for (avatarName in allAvatars) {
            val resId = resources.getIdentifier(avatarName, "drawable", requireContext().packageName)
            if (resId != 0) {
                prizes.add(Prize(PrizeType.AVATAR, avatarName, resId, 50 / allAvatars.size)) // Distribuye 50% entre avatares
            }
        }
        // Premios de dinero (50% de probabilidad total)
        prizes.add(Prize(PrizeType.MONEY, 0.50, R.drawable.ic_coin_small, 25)) // 25% para 0.30€
        prizes.add(Prize(PrizeType.MONEY, 2.00, R.drawable.ic_coin_medium, 15)) // 15% para 0.50€
        prizes.add(Prize(PrizeType.MONEY, 5.00, R.drawable.ic_coin_large, 10))  // 10% para 1.00€
        // Asegúrate de que los pesos sumen 100
        // Si tienes más avatares, ajusta los pesos de dinero para que la suma total sea 100.
        // Ejemplo: si tienes 6 avatares (6*8.33 = 50%), entonces dinero=50%.
    }

    private fun setupClickListeners() {
        binding.btnOpenLootbox.setOnClickListener {
            if (currentUserId == null) {
                Toast.makeText(requireContext(), "Inicia sesión para abrir la kutxa.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkUserBalanceAndOpenLootbox()
        }
    }

    private fun checkUserBalanceAndOpenLootbox() {
        db.collection("Users").document(currentUserId!!).get()
            .addOnSuccessListener { document ->
                // LEER COMO STRING Y CONVERTIR A DOUBLE
                val dineroStr = document.getString("dinero") ?: "0.0"
                val currentBalance = dineroStr.toDoubleOrNull() ?: 0.0

                val cost = 1.00 // Coste de la caja

                if (currentBalance >= cost) {
                    val nuevoSaldo = currentBalance - cost

                    // GUARDAR DE NUEVO COMO STRING
                    db.collection("Users").document(currentUserId!!)
                        .update("dinero", nuevoSaldo.toString())
                        .addOnSuccessListener {
                            openLootbox()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Errorea saldoa eguneratzean.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Saldo nahikoa ez. $cost € behar duzu.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun generateCarouselItems() {
        binding.lootboxCarousel.removeAllViews() // Limpiar items anteriores

        val random = Random()
        // Generamos un montón de ítems aleatorios para el carrusel
        for (i in 0 until 50) { // Suficientes ítems para un buen desplazamiento
            val randomPrize = prizes[random.nextInt(prizes.size)]
            val imageView = createCarouselImageView(randomPrize.drawableResId)
            binding.lootboxCarousel.addView(imageView)
        }
    }

    private fun createCarouselImageView(drawableResId: Int): ImageView {
        val imageView = ImageView(requireContext())
        val density = resources.displayMetrics.density

        val sizePx = (110 * density).toInt()
        val marginPx = (10 * density).toInt() // <--- Importante: 10dp

        val layoutParams = LinearLayout.LayoutParams(sizePx, sizePx)
        layoutParams.setMargins(marginPx, 0, marginPx, 0) // Márgenes laterales exactos

        imageView.layoutParams = layoutParams
        imageView.setImageResource(drawableResId)
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        imageView.setBackgroundResource(R.drawable.bg_item_lootbox)

        return imageView
    }


    private fun getRandomPrizeByWeight(): Prize {
        val totalWeight = prizes.sumOf { it.weight }
        val randomNum = Random().nextInt(totalWeight) + 1 // +1 para que el rango sea 1 a totalWeight

        var currentWeight = 0
        for (prize in prizes) {
            currentWeight += prize.weight
            if (randomNum <= currentWeight) {
                return prize
            }
        }
        // Fallback, esto no debería pasar si los pesos están bien definidos
        return prizes.first()
    }

    private fun processPrize(prize: Prize) {
        // IMPORTANTE: Aquí usamos 'prize.value', que es el valor del objeto que cayó en la flecha
        when (prize.type) {
            PrizeType.AVATAR -> {
                val avatarName = prize.value.toString()
                addAvatarToUser("$avatarName.png")
            }
            PrizeType.MONEY -> {
                val amount = prize.value as Double
                addMoneyToUser(amount)
            }
        }
        showPrizeDialog(prize)
    }

    private fun openLootbox() {
        binding.btnOpenLootbox.isEnabled = false
        binding.tvPrizeHint.text = "Zorte on! Kutxa irekitzen..."

        // 1. Elegimos el premio ganador REAL primero
        val winningPrize = getRandomPrizeByWeight()

        binding.lootboxCarousel.removeAllViews()
        val random = java.util.Random()

        // 2. FIJAMOS la cantidad de ítems anteriores para que el cálculo sea exacto
        val numPrecedingItems = 30

        // 3. Añadimos ítems aleatorios visuales antes del ganador
        for (i in 0 until numPrecedingItems) {
            val randomPrize = prizes[random.nextInt(prizes.size)]
            binding.lootboxCarousel.addView(createCarouselImageView(randomPrize.drawableResId))
        }

        // 4. Añadimos el premio ganador REAL que elegimos al principio
        binding.lootboxCarousel.addView(createCarouselImageView(winningPrize.drawableResId))

        // 5. Añadimos ítems después para que no se vea vacío al final
        for (i in 0 until 10) {
            val randomPrize = prizes[random.nextInt(prizes.size)]
            binding.lootboxCarousel.addView(createCarouselImageView(randomPrize.drawableResId))
        }

        // 6. CÁLCULO MATEMÁTICO EXACTO DEL DESPLAZAMIENTO
        // Cada ítem mide 110dp + 10dp marginStart + 10dp marginEnd = 130dp totales
        val density = resources.displayMetrics.density
        val itemWidthPx = (130 * density).toInt()

        // Calculamos el centro exacto del contenedor
        val containerWidth = binding.lootboxCarouselContainer.width

        // La fórmula mágica: (items anteriores * ancho) - (mitad del contenedor) + (mitad de un ítem para centrarlo)
        val targetScrollX = (numPrecedingItems * itemWidthPx) - (containerWidth / 2) + (itemWidthPx / 2)

        // 7. Animación sincronizada
        val animator = ObjectAnimator.ofInt(
            binding.lootboxCarousel,
            "scrollX",
            0, // Empezar siempre desde el principio
            targetScrollX
        )
        animator.duration = 5000 // 5 segundos de tensión
        animator.interpolator = EaseOutCubicInterpolator()
        animator.start()

        // 8. Procesar el premio ganador que pusimos en la imagen
        Handler(Looper.getMainLooper()).postDelayed({
            processPrize(winningPrize) // <--- Aquí pasamos el objeto EXACTO que creamos arriba
            binding.btnOpenLootbox.isEnabled = true
            binding.tvPrizeHint.text = "¡Zorionak! Saria lortu duzu."
        }, animator.duration + 500)
    }
    private fun showPrizeDialog(prize: Prize) {
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_prize_winner)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false) // Obliga a darle al botón

        val tvPrizeName = dialog.findViewById<TextView>(R.id.tvPrizeName)
        val ivPrizeIcon = dialog.findViewById<ImageView>(R.id.ivPrizeIcon)
        val btnCollect = dialog.findViewById<Button>(R.id.btnCollect)
        val viewGlow = dialog.findViewById<View>(R.id.viewGlow)

        // Configurar contenido
        if (prize.type == PrizeType.AVATAR) {
            tvPrizeName.text = "Avatar berria: ${prize.value}"
        } else {
            tvPrizeName.text = "Dirua lortu duzu: ${prize.value} €"
        }
        ivPrizeIcon.setImageResource(prize.drawableResId)

        // Animación de latido en el brillo
        val scaleX = ObjectAnimator.ofFloat(viewGlow, "scaleX", 0.8f, 1.2f, 0.8f)
        val scaleY = ObjectAnimator.ofFloat(viewGlow, "scaleY", 0.8f, 1.2f, 0.8f)
        scaleX.repeatCount = ObjectAnimator.INFINITE
        scaleY.repeatCount = ObjectAnimator.INFINITE
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 2000
            start()
        }

        btnCollect.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun addAvatarToUser(avatarNameWithPng: String) {
        db.collection("Users").document(currentUserId!!)
            .update("avatares", FieldValue.arrayUnion(avatarNameWithPng))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Avatarra bildumara gehituta.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Errorea avatarra gehitzean.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addMoneyToUser(amount: Double) {
        val userRef = db.collection("Users").document(currentUserId!!)

        userRef.get().addOnSuccessListener { document ->
            val dineroStr = document.getString("dinero") ?: "0.0"
            // Reemplazamos coma por punto por si el sistema está en español/euskera
            val currentBalance = dineroStr.replace(",", ".").toDoubleOrNull() ?: 0.0
            val nuevoTotal = currentBalance + amount

            // FORMATEO CRÍTICO: "%.2f" asegura 2 decimales y usamos Locale.US para el punto (.)
            val dineroFormateado = String.format(java.util.Locale.US, "%.2f", nuevoTotal)

            userRef.update("dinero", dineroFormateado)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "$amount € gehitu dira!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Interpolador para una animación más suave (frenado gradual)
    class EaseOutCubicInterpolator : android.view.animation.Interpolator {
        override fun getInterpolation(input: Float): Float {
            // (1-x)^3
            val x = 1 - input
            return (1 - (x * x * x))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}