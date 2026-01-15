package com.example.musroyale

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.musroyale.databinding.ActivityCryptoPaymentBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore

class CryptoPaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCryptoPaymentBinding
    private val db = FirebaseFirestore.getInstance()
    private var mode = "pendiente" // "pendiente" para añadir, "retirada" para sacar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCryptoPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("userRegistrado", null)
        val username = prefs.getString("username", "Usuario")

        binding.btnBack.setOnClickListener { finish() }

        // --- LÓGICA DE PESTAÑAS ---
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    // MODO GORDAILUA (Añadir)
                    mode = "pendiente"
                    binding.txtTitle.text = "Dirua Sartu"
                    binding.cardWalletInfo.visibility = View.VISIBLE
                    binding.layoutMonto.hint = "Zenbat USDT bidali dituzu?"
                    binding.layoutID.hint = "Binance ID edo Tx ID"
                    binding.btnConfirmarAccion.text = "Bidali Gordailua"
                } else {
                    // MODO ERRETIRATU (Retirar)
                    mode = "retirada"
                    binding.txtTitle.text = "Erretiratu Dirua"
                    binding.cardWalletInfo.visibility = View.GONE
                    binding.layoutMonto.hint = "Zenbat USDT atera nahi dituzu?"
                    binding.layoutID.hint = "Zure Wallet helbidea (BEP20)"
                    binding.btnConfirmarAccion.text = "Eskatu Erretiratzea"
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // --- LÓGICA DE SELECCIÓN DE COINS ---
        val btcAddress = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
        val ethAddress = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e"
        val usdtAddress = "0x742d35Cc6634C0532925a3b844Bc454e4438f44e"

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

        binding.btnCopiarWallet.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Wallet", binding.txtWalletAddress.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Helbidea kopiatuta!", Toast.LENGTH_SHORT).show()
        }

        // --- BOTÓN CONFIRMAR ACCIÓN (Sube a Firebase) ---
        binding.btnConfirmarAccion.setOnClickListener {
            val montoStr = binding.editMontoEnviado.text.toString()
            val userRefId = binding.editBinanceIDUser.text.toString() // Puede ser TxID o Wallet de destino

            if (montoStr.isNotEmpty() && userRefId.isNotEmpty() && currentUserId != null) {
                val solicitud = hashMapOf(
                    "userId" to currentUserId,
                    "username" to username,
                    "monto" to montoStr.toDouble(),
                    "orderId" to userRefId,
                    "status" to mode, // Aquí cambiará entre "pendiente" o "retirada"
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("SolicitudesRecarga").add(solicitud).addOnSuccessListener {
                    showCustomRevisionDialog()
                }
            } else {
                Toast.makeText(this, "Mesedez, osatu datu guztiak", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showCustomRevisionDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.layout_dialog_revision)

        // Hacer el fondo del diálogo transparente para que se vea el redondeado
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        val tvTitle = dialog.findViewById<android.widget.TextView>(R.id.tvDialogTitle)
        val tvMessage = dialog.findViewById<android.widget.TextView>(R.id.tvDialogMessage)
        val btnOk = dialog.findViewById<android.widget.Button>(R.id.btnDialogOk)

        // Ajustar textos según el modo
        if (mode == "retirada") {
            tvTitle.text = "Erretiratzea Berrikusten"
            tvMessage.text = "Zure erretiratze eskaera jaso dugu. Saldoa egiaztatu ondoren, transferentzia prozesatuko dugu."
        }

        btnOk.setOnClickListener {
            dialog.dismiss()
            finish() // Volver atrás después de aceptar
        }

        dialog.show()
    }
}