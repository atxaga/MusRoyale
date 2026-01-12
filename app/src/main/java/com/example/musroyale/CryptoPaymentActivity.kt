package com.example.musroyale

import android.content.ClipData
import android.content.ClipboardManager // <--- IMPORTACIÓN FALTANTE
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.musroyale.databinding.ActivityCryptoPaymentBinding
import com.google.firebase.firestore.FirebaseFirestore

class CryptoPaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCryptoPaymentBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCryptoPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("userRegistrado", null)
        val username = prefs.getString("username", "Usuario")

        binding.btnBack.setOnClickListener {
            finish() // Esto cierra la actividad actual y vuelve a la anterior
        }
        // Botón para copiar el ID
        val btcAddress = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
        val ethAddress = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e"
        val usdtAddress = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e" // BEP20 suele ser igual que ETH

        binding.btnSelectBTC.setOnClickListener {
            binding.txtWalletAddress.text = btcAddress
            binding.txtLabelRed.text = "Sarea: Bitcoin"
            binding.txtLabelRed.setTextColor(Color.parseColor("#F7931A"))
        }

        binding.btnSelectETH.setOnClickListener {
            binding.txtWalletAddress.text = ethAddress
            binding.txtLabelRed.text = "Sarea: Ethereum (ERC20)"
            binding.txtLabelRed.setTextColor(Color.parseColor("#627EEA"))
        }

        binding.btnSelectUSDT.setOnClickListener {
            binding.txtWalletAddress.text = usdtAddress
            binding.txtLabelRed.text = "Sarea: BNB Smart Chain (BEP20)"
            binding.txtLabelRed.setTextColor(Color.parseColor("#26A17B"))
        }

// El botón de copiar siempre copiará lo que esté escrito en el TextView actual
        binding.btnCopiarWallet.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Wallet", binding.txtWalletAddress.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Helbidea kopiatuta!", Toast.LENGTH_SHORT).show()
        }

        // Botón para enviar la solicitud de recarga
        binding.btnConfirmarEnvio.setOnClickListener {
            val montoStr = binding.editMontoEnviado.text.toString()
            val txId = binding.editBinanceIDUser.text.toString()

            if (montoStr.isNotEmpty() && txId.isNotEmpty() && currentUserId != null) {
                val solicitud = hashMapOf(
                    "userId" to currentUserId,
                    "username" to username, // Corregido: antes decía usernameLocal
                    "monto" to montoStr.toDouble(),
                    "orderId" to txId,
                    "status" to "pendiente",
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("SolicitudesRecarga").add(solicitud).addOnSuccessListener {
                    Toast.makeText(this, "Eskerrik asko! Datuak egiaztatuko ditugu.", Toast.LENGTH_LONG).show()
                    finish()
                }
            } else {
                Toast.makeText(this, "Mesedez, osatu datu guztiak", Toast.LENGTH_SHORT).show()
            }
        }
    }
}