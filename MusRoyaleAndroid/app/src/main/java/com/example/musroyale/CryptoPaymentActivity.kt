package com.example.musroyale

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musroyale.databinding.ActivityCryptoPaymentBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore

class CryptoPaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCryptoPaymentBinding
    private val db = FirebaseFirestore.getInstance()
    private var mode = "pendiente"

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
                when (tab?.position) {
                    0 -> { // MODO GORDAILUA
                        mode = "pendiente"
                        actualizarVisibilidad(esHistorial = false)
                        binding.txtTitle.text = "Dirua Sartu"
                        binding.cardWalletInfo.visibility = View.VISIBLE
                        binding.layoutMonto.hint = "Zenbat USDT bidali dituzu?"
                        binding.layoutID.hint = "Binance ID edo Tx ID"
                        binding.btnConfirmarAccion.text = "Bidali Gordailua"
                    }
                    1 -> { // MODO ERRETIRATU
                        mode = "retirada"
                        actualizarVisibilidad(esHistorial = false)
                        binding.txtTitle.text = "Erretiratu Dirua"
                        binding.cardWalletInfo.visibility = View.GONE
                        binding.layoutMonto.hint = "Zenbat USDT atera nahi dituzu?"
                        binding.layoutID.hint = "Zure Wallet helbidea (BEP20)"
                        binding.btnConfirmarAccion.text = "Eskatu Erretiratzea"
                    }
                    2 -> { // MODO HISTORIALA
                        actualizarVisibilidad(esHistorial = true)
                        binding.txtTitle.text = "Nire Historiala"
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // --- SELECCIÓN DE COINS ---
        configurarSeleccionCoins()

        // --- BOTÓN CONFIRMAR ---
        binding.btnConfirmarAccion.setOnClickListener {
            val montoStr = binding.editMontoEnviado.text.toString()
            val userRefId = binding.editBinanceIDUser.text.toString()
            val montoSolicitado = montoStr.toDoubleOrNull() ?: 0.0

            if (montoStr.isNotEmpty() && userRefId.isNotEmpty() && currentUserId != null) {
                if (mode == "retirada") {
                    db.collection("Users").document(currentUserId).get().addOnSuccessListener { doc ->
                        val saldo = doc.getString("dinero")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                        if (montoSolicitado > saldo) {
                            Toast.makeText(this, "Errorea: Saldo nahikorik ez", Toast.LENGTH_LONG).show()
                        } else {
                            val nuevoSaldo = String.format(java.util.Locale.US, "%.2f", saldo - montoSolicitado)
                            db.collection("Users").document(currentUserId).update("dinero", nuevoSaldo)
                                .addOnSuccessListener { subirSolicitud(currentUserId, username!!, montoSolicitado, userRefId) }
                        }
                    }
                } else {
                    subirSolicitud(currentUserId, username!!, montoSolicitado, userRefId)
                }
            } else {
                Toast.makeText(this, "Mesedez, osatu datu guztiak", Toast.LENGTH_SHORT).show()
            }
        }

        // --- CONFIG RECYCLER ---
        binding.rvHistorial.layoutManager = LinearLayoutManager(this)
        cargarHistorial(currentUserId)
    }

    private fun actualizarVisibilidad(esHistorial: Boolean) {
        val formVisibility = if (esHistorial) View.GONE else View.VISIBLE
        val historyVisibility = if (esHistorial) View.VISIBLE else View.GONE

        binding.cardFormulario.visibility = formVisibility
        binding.cardWalletInfo.visibility = if (mode == "pendiente" && !esHistorial) View.VISIBLE else View.GONE
        binding.rvHistorial.visibility = historyVisibility
    }

    private fun subirSolicitud(userId: String, username: String, monto: Double, orderId: String) {
        val solicitud = hashMapOf(
            "userId" to userId,
            "username" to username,
            "monto" to monto,
            "orderId" to orderId,
            "status" to mode,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("SolicitudesRecarga").add(solicitud).addOnSuccessListener {
            showCustomRevisionDialog()
        }
    }

    private fun cargarHistorial(userId: String?) {
        if (userId == null) return

        // Filtramos para que solo aparezcan las que están en proceso de revisión
        db.collection("SolicitudesRecarga")
            .whereEqualTo("userId", userId)
            .whereIn("status", listOf("pendiente", "retirada"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreError", "Errorea kargatzerakoan: ${error.message}")
                    return@addSnapshotListener
                }

                val lista = snapshot?.map { doc ->
                    SolicitudPago(
                        idDoc = doc.id,
                        userId = doc.getString("userId") ?: "",
                        username = doc.getString("username") ?: "",
                        monto = doc.getDouble("monto") ?: 0.0,
                        orderId = doc.getString("orderId") ?: "",
                        status = doc.getString("status") ?: "pendiente"
                    )
                }?.sortedByDescending { it.idDoc } ?: listOf()

                binding.rvHistorial.adapter = HistorialAdapter(lista) { docId ->
                    mostrarDialogoConfirmacionBorrado(docId)
                }
            }
    }
    private fun mostrarDialogoConfirmacionBorrado(docId: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Eskaera Ezeztatu")
            .setMessage("Ziur zaude eskaera hau ezabatu nahi duzula?")
            .setPositiveButton("Bai, Ezabatu") { _, _ ->

                val docRef = db.collection("SolicitudesRecarga").document(docId)

                docRef.get().addOnSuccessListener { snapshot ->
                    val status = snapshot.getString("status") ?: ""
                    val monto = snapshot.getDouble("monto") ?: 0.0
                    val userId = snapshot.getString("userId") ?: ""

                    if (status == "retirada") {
                        // Si era un retiro, le devolvemos el dinero al usuario antes de borrar
                        val userRef = db.collection("Users").document(userId)
                        db.runTransaction { transaction ->
                            val userSnap = transaction.get(userRef)
                            val dineroActual = userSnap.getString("dinero")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                            val nuevoSaldo = String.format(java.util.Locale.US, "%.2f", dineroActual + monto)

                            transaction.update(userRef, "dinero", nuevoSaldo)
                            transaction.delete(docRef)
                            null
                        }.addOnSuccessListener {
                            Toast.makeText(this, "Eskaera ezeztatua eta dirua itzulia", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Si era un depósito (pendiente), solo borramos el documento
                        docRef.delete().addOnSuccessListener {
                            Toast.makeText(this, "Eskaera ezabatuta", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Utzi", null)
            .show()
    }

    private fun borrarSolicitud(docId: String) {
        db.collection("SolicitudesRecarga").document(docId).delete()
            .addOnSuccessListener { Toast.makeText(this, "Ezabatuta", Toast.LENGTH_SHORT).show() }
    }

    private fun configurarSeleccionCoins() {
        binding.btnSelectBTC.setOnClickListener { actualizarWallet("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa", "Bitcoin", "#F7931A") }
        binding.btnSelectETH.setOnClickListener { actualizarWallet("0x742d...", "Ethereum (ERC20)", "#627EEA") }
        binding.btnSelectUSDT.setOnClickListener { actualizarWallet("0x742d...", "BSC (BEP20)", "#26A17B") }
        binding.btnCopiarWallet.setOnClickListener {
            val clip = ClipData.newPlainText("Wallet", binding.txtWalletAddress.text)
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
            Toast.makeText(this, "Kopiatuta!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarWallet(addr: String, red: String, color: String) {
        binding.txtWalletAddress.text = addr
        binding.txtLabelRed.text = "Sarea: $red"
        binding.txtLabelRed.setTextColor(Color.parseColor(color))
    }

    private fun showCustomRevisionDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.layout_dialog_revision)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val tvTitle = dialog.findViewById<android.widget.TextView>(R.id.tvDialogTitle)
        val btnOk = dialog.findViewById<android.widget.Button>(R.id.btnDialogOk)

        tvTitle.text = if (mode == "retirada") "Erretiratzea Berrikusten" else "Gordailua Berrikusten"
        btnOk.setOnClickListener {
            dialog.dismiss()
            finish()
        }
        dialog.show()
    }
}