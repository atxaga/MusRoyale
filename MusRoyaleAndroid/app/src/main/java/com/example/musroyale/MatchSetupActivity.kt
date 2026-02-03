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

        binding.btnBack.setOnClickListener {
            finish()

        }
        binding.btnPlay.setOnClickListener {
            val valueToSend = "PUBLICA"
            val intent = Intent(this, PartidaActivity::class.java)
            intent.putExtra(PartidaActivity.EXTRA_PARAM, valueToSend)
            startActivity(intent)
        }
        setupApuestaLogica()


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
