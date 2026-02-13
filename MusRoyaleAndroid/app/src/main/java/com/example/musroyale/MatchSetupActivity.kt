package com.example.musroyale

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.musroyale.databinding.ActivityMatchSetupBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.snackbar.Snackbar

class MatchSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMatchSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_match_setup)

        binding = ActivityMatchSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupApuestaLogica()

        binding.btnBack.setOnClickListener {
            finish()

        }
        binding.btnPlay.setOnClickListener {
            // 1. Determinar el modo de juego


            // 2. Leer la apuesta como número (Double para comparar con el saldo)
            val apuesta = binding.etApuestaValue.text.toString().toDoubleOrNull() ?: 0.0

            val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val uid = prefs.getString("userRegistrado", "") ?: ""

            if (uid.isEmpty()) return@setOnClickListener

            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("Users").document(uid).get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {

                    val dineroStr = document.getString("dinero") ?: "0"
                    val dineroActual = dineroStr.replace(",", ".").toDoubleOrNull() ?: 0.0

                    if (dineroActual >= apuesta) {
                        val intent = Intent(this, PartidaActivity::class.java).apply {
                            putExtra(PartidaActivity.EXTRA_PARAM, "PUBLICA")
                            putExtra("APUESTA_CANTIDAD", apuesta.toInt())
                        }
                        startActivity(intent)
                    } else {
                        // NO TIENE SALDO: Formateamos el mensaje para que se vea bien (ej: 17.70)
                        val saldoFormateado = "%.2f".format(java.util.Locale.US, dineroActual)
                        Snackbar.make(
                            binding.root,
                            "Ez duzu nahiko diru! (Saldo: $saldoFormateado €)",
                            Snackbar.LENGTH_LONG
                        ).setBackgroundTint(
                            ContextCompat.getColor(
                                this,
                                android.R.color.holo_red_dark
                            )
                        )
                            .show()
                    }

                } else {
                    Snackbar.make(
                        binding.root,
                        "Errorea: Erabiltzailea ez da aurkitu",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }.addOnFailureListener { e ->
                Snackbar.make(binding.root, "Errorea konexioan: ${e.message}", Snackbar.LENGTH_LONG)
                    .show()
            }
        }
    }
    // En tu Activity
    private fun setupApuestaLogica() {
        val etApuesta = binding.etApuestaValue

        // Botón MENOS
        binding.btnMinus.setOnClickListener {
            val actual = etApuesta.text.toString().toIntOrNull() ?: 0
            if (actual >= 5) {
                etApuesta.setText((actual - 5).toString())
            } else {
                etApuesta.setText("0")
            }
        }

        // Botón MÁS
        binding.btnPlus.setOnClickListener {
            val actual = etApuesta.text.toString().toIntOrNull() ?: 0
            etApuesta.setText((actual + 5).toString())
        }

        // Opcional: Que al borrar todo no se quede vacío
        etApuesta.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && etApuesta.text.isEmpty()) {
                etApuesta.setText("0")
            }
        }
    }


}
