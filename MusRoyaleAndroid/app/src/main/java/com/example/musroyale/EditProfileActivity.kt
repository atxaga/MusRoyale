package com.example.musroyale

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.musroyale.databinding.ActivityEditProfileBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private var userId: String? = null
    private var runnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId = prefs.getString("userRegistrado", null)

        loadProfile()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSaveProfile.setOnClickListener { saveProfile() }

        // Listener para el botón de suscripción azul
        binding.btnSubscribe.setOnClickListener {
            showSubscriptionDialog()
        }
    }

    private fun showSubscriptionDialog() {
        val dialog = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.dialog_subscription, null)

        dialog.setView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmSub)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancelSub)

        btnConfirm.setOnClickListener {
            ejecutarCompraPremium(dialog)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun ejecutarCompraPremium(dialog: AlertDialog) {
        val id = userId ?: return
        val userRef = db.collection("Users").document(id)

        Toast.makeText(this, "Transakzioa prozesatzen...", Toast.LENGTH_SHORT).show()

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val saldoActual = snapshot.getString("dinero")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
            val oroActual = (snapshot.getString("oro") ?: "0").toInt()

            if (saldoActual >= 10.0) {
                val nuevoSaldo = saldoActual - 10.0
                val nuevoOro = oroActual + 50000

                val timestampActual = System.currentTimeMillis()
                transaction.update(userRef, "dinero", String.format("%.2f", nuevoSaldo).replace(",", "."))
                transaction.update(userRef, "oro", nuevoOro.toString())
                transaction.update(userRef, "premium", true)
                transaction.update(userRef, "ultimaRecargaOro", timestampActual)
                true
            } else {
                throw Exception("SALDO_INSUFICIENTE")
            }
        }.addOnSuccessListener {
            dialog.dismiss()
            Toast.makeText(this, "¡Premium aktibatuta! 🏆 +50.000 urre", Toast.LENGTH_LONG).show()
            loadProfile() // Recargamos para ver el nuevo saldo
        }.addOnFailureListener { e ->
            val msg = if (e.message == "SALDO_INSUFICIENTE") "Ez duzu nahikoa diru (10€ behar dira)" else "Errorea: ${e.message}"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfile() {
        val id = userId ?: return

        // Cancelamos listener previo si existiera para evitar duplicados
        userListener?.remove()

        userListener = db.collection("Users").document(id)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Errorea dautak jasotzean", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // 1. Datos básicos
                    binding.etEditUsername.setText(snapshot.getString("username") ?: "")
                    binding.etEditEmail.setText(snapshot.getString("email") ?: "")
                    val saldo = snapshot.getString("dinero") ?: "0.00"
                    binding.tvMoney.text = "$saldo €"

                    // 2. Gestión de Premium en Tiempo Real
                    val esPremium = snapshot.getBoolean("premium") ?: false

                    if (esPremium) {
                        val ultimaRecarga = snapshot.getLong("ultimaRecargaOro") ?: 0L
                        iniciarContadorRealTime(id, snapshot.getString("oro") ?: "0", ultimaRecarga)
                    } else {
                        detenerContador()
                        resetearBotonSuscripcion()
                    }

                    // 3. Avatar
                    val nombreImagen = snapshot.getString("avatarActual")
                    if (!nombreImagen.isNullOrEmpty()) {
                        val resId = resources.getIdentifier(nombreImagen.replace(".png", ""), "drawable", packageName)
                        if (resId != 0) binding.ivAvatar.setImageResource(resId)
                    }
                }
            }
    }
    private fun iniciarContadorRealTime(id: String, oroActual: String, ultimaRecarga: Long) {
        binding.btnUnsubscribe.visibility = android.view.View.VISIBLE
        binding.btnUnsubscribe.setOnClickListener { mostrarDialogoCancelar() }

        runnable?.let { handler.removeCallbacks(it) }
        runnable = object : Runnable {
            override fun run() {
                val tiempoTexto = obtenerCuentaAtras(ultimaRecarga)
                if (tiempoTexto == "RECARGA DISPONIBLE") {
                    recargarOroMensual(id, oroActual.toInt())
                } else {
                    binding.btnSubscribe.isEnabled = false
                    binding.btnSubscribe.text = tiempoTexto
                    binding.btnSubscribe.setBackgroundColor(Color.parseColor("#455A64"))
                    binding.btnSubscribe.setIconResource(0)
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(runnable!!)
    }

    private fun detenerContador() {
        runnable?.let { handler.removeCallbacks(it) }
        runnable = null
    }

    private fun resetearBotonSuscripcion() {
        binding.btnSubscribe.isEnabled = true
        binding.btnSubscribe.text = "HARPIDETU (PREMIUM)"
        binding.btnSubscribe.setBackgroundResource(R.drawable.bg_subscription_gold)
        binding.btnSubscribe.setBackgroundTintList(null)
        binding.btnSubscribe.setIconResource(android.R.drawable.btn_star_big_on)
        binding.btnSubscribe.visibility = android.view.View.VISIBLE
        binding.btnUnsubscribe.visibility = android.view.View.GONE
    }
    private fun mostrarDialogoCancelar() {
        val dialog = AlertDialog.Builder(this).create()
        val view = layoutInflater.inflate(R.layout.dialog_cancel_premium, null)

        dialog.setView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnStay = view.findViewById<Button>(R.id.btnStayPremium)
        val btnConfirmUnsub = view.findViewById<TextView>(R.id.btnConfirmCancel)

        // Si decide quedarse, simplemente cerramos el diálogo
        btnStay.setOnClickListener {
            dialog.dismiss()
        }

        // Si confirma la cancelación
        btnConfirmUnsub.setOnClickListener {
            cancelarPremium()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun cancelarPremium() {
        userId?.let { id ->
            val updates = mapOf(
                "premium" to false,
                "ultimaRecargaOro" to 0L
            )

            db.collection("Users").document(id).update(updates)
                .addOnSuccessListener {
                    runnable?.let { handler.removeCallbacks(it) } // Paramos el reloj
                    Toast.makeText(this, "Harpidetza bertan behera utzi da", Toast.LENGTH_SHORT).show()

                    resetearBotonSuscripcion() // <--- CAMBIO AQUÍ: UI instantánea
                }
        }
    }
    private fun recargarOroMensual(id: String, oroActual: Int) {
        val nuevoOro = oroActual + 50000
        val updates = mapOf(
            "oro" to nuevoOro.toString(),
            "ultimaRecargaOro" to System.currentTimeMillis()
        )
        db.collection("Users").document(id).update(updates).addOnSuccessListener {
            Toast.makeText(this, "¡Hileroko 50.000 urre jaso dituzu! 💰", Toast.LENGTH_LONG).show()
            loadProfile() // Recargar para actualizar textos
        }
    }
    private fun obtenerCuentaAtras(lastRecarga: Long): String {
        val unMesEnMillis = 30L * 24 * 60 * 60 * 1000
        val proximaRecarga = lastRecarga + unMesEnMillis
        val tiempoRestante = proximaRecarga - System.currentTimeMillis()

        if (tiempoRestante <= 0) return "RECARGA DISPONIBLE"

        val dias = tiempoRestante / (24 * 60 * 60 * 1000)
        val horas = (tiempoRestante % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
        val minutos = (tiempoRestante % (60 * 60 * 1000)) / (60 * 1000)
        val segundos = (tiempoRestante % (60 * 1000)) / 1000

        // Formato: 28d 12h 30m 45s
        return "Urrea: ${dias}d ${horas}h ${minutos}m ${segundos}s"
    }


    private fun saveProfile() {
        val nuevoUser = binding.etEditUsername.text.toString().trim()
        val nuevoEmail = binding.etEditEmail.text.toString().trim()
        val pass = binding.etEditPassword.text.toString()

        if (nuevoUser.isEmpty()) {
            binding.etEditUsername.error = "Izena ezin da hutsik egon"
            return
        }

        val updates = mutableMapOf<String, Any>("username" to nuevoUser, "email" to nuevoEmail)

        if (pass.isNotEmpty()) {
            if (pass.length < 6) {
                binding.etEditPassword.error = "Gutxienez 6 karaktere"
                return
            }
            updates["password"] = sha256(pass)
        }

        userId?.let { id ->
            db.collection("Users").document(id).update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profila eguneratuta!", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }

    private fun sha256(base: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(base.toByteArray(charset("UTF-8")))
        return hash.joinToString("") { "%02x".format(it) }
    }
    override fun onDestroy() {
        super.onDestroy()
        runnable?.let { handler.removeCallbacks(it) }
    }
}