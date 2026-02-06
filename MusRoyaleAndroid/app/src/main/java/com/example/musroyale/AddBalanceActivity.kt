package com.example.musroyale

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.musroyale.databinding.ActivityAddBalanceBinding
import com.google.firebase.firestore.FirebaseFirestore

class AddBalanceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddBalanceBinding
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBalanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        currentUserId = prefs.getString("userRegistrado", null)

        cargarSaldoActual()

        binding.atzeraBueltatu.setOnClickListener {
            finish()
        }

        binding.bottomLeftButton.setOnClickListener {
            val cantidadRaw = binding.etAmount.text.toString()
            val cantidadLimpia = cantidadRaw.replace(",", ".")

            if (cantidadLimpia.isNotEmpty() && cantidadLimpia.toDoubleOrNull() != null) {
                val urlDePago = "https://bittortelletxea.github.io/mus-royale-pago/?amount=$cantidadLimpia&userId=$currentUserId"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlDePago))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Idatzi zenbat diru sartu nahi duzun", Toast.LENGTH_SHORT).show()
            }
        }

        procesarRetornoDePago(intent)
    }


    private fun cargarSaldoActual() {
        val uid = currentUserId ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("Users").document(uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val saldo = snapshot.getString("dinero") ?: "0"
                    binding.textBalanceAmount.text = "$saldo €"
                    binding.rightText.text = "$saldo €"
                }
            }
    }

    private fun procesarRetornoDePago(intent: Intent) {
        val data: Uri? = intent.data
        if (data != null && data.toString().contains("pago_exitoso")) {
            val monto = data.getQueryParameter("monto")
            actualizarSaldoEnFirebase(monto)
            intent.data = null
        }
    }

    private fun actualizarSaldoEnFirebase(montoASumar: String?) {
        val uid = currentUserId ?: return
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        val montoNumerico = montoASumar?.toDoubleOrNull() ?: 0.0

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val saldoActualString = snapshot.getString("dinero") ?: "0"
            val saldoActualNumerico = saldoActualString.toDoubleOrNull() ?: 0.0

            val nuevoSaldoNumerico = saldoActualNumerico + montoNumerico
            val nuevoSaldoString = nuevoSaldoNumerico.toString()

            transaction.update(userRef, "dinero", nuevoSaldoString)
            nuevoSaldoString
        }.addOnSuccessListener { nuevoSaldoFinal ->
            binding.textBalanceAmount.text = "$nuevoSaldoFinal €"
            binding.rightText.text = "$nuevoSaldoFinal €"
            Toast.makeText(this, "Eskerrik asko! $montoASumar € sartu dira.", Toast.LENGTH_LONG).show()
        }
    }
}