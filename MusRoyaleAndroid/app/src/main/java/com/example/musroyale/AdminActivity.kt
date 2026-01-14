package com.example.musroyale

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musroyale.databinding.ActivityAdminPaymentsBinding
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminPaymentsBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPaymentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // Configuramos el RecyclerView una sola vez
        binding.rvSolicitudes.layoutManager = LinearLayoutManager(this)

        cargarSolicitudes()
    }

    private fun cargarSolicitudes() {
        db.collection("SolicitudesRecarga")
            .whereIn("status", listOf("pendiente", "retirada"))
            .addSnapshotListener { snapshot, _ ->
                val lista = snapshot?.map { doc ->
                    SolicitudPago(
                        idDoc = doc.id,
                        userId = doc.getString("userId") ?: "",
                        username = doc.getString("username") ?: "",
                        monto = doc.getDouble("monto") ?: 0.00,
                        orderId = doc.getString("orderId") ?: "",
                        status = doc.getString("status") ?: "pendiente"
                    )
                } ?: listOf()

                // PASAMOS AMBAS FUNCIONES AL ADAPTADOR
                binding.rvSolicitudes.adapter = AdminPagosAdapter(
                    lista,
                    onAprobarClick = { solicitud -> confirmarAccion(solicitud) },
                    onRechazarClick = { solicitud -> rechazarSolicitud(solicitud) }
                )
            }
    }

    private fun confirmarAccion(solicitud: SolicitudPago) {
        val userRef = db.collection("Users").document(solicitud.userId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val dineroActualString = document.getString("dinero") ?: "0.00"
                val dineroActualDouble = dineroActualString.replace(",", ".").toDoubleOrNull() ?: 0.00

                val nuevoSaldo = if (solicitud.status == "retirada") {
                    dineroActualDouble - solicitud.monto
                } else {
                    dineroActualDouble + solicitud.monto
                }

                if (nuevoSaldo < 0 && solicitud.status == "retirada") {
                    Toast.makeText(this, "Errorea: Erabiltzaileak ez du saldo nahikorik", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val saldoFormateado = String.format(java.util.Locale.US, "%.2f", nuevoSaldo)

                userRef.update("dinero", saldoFormateado)
                    .addOnSuccessListener {
                        db.collection("SolicitudesRecarga").document(solicitud.idDoc)
                            .update("status", "aprobado")

                        val msg = if (solicitud.status == "retirada") "Erretiratzea onartua" else "Karga onartua"
                        Toast.makeText(this, "$msg: ${solicitud.username}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun rechazarSolicitud(solicitud: SolicitudPago) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Eskaera Ukatu")
            .setMessage("Ziur zaude ${solicitud.username}(r)en eskaera ukatu nahi duzula?")
            .setPositiveButton("Bai, Ukatu") { _, _ ->
                db.collection("SolicitudesRecarga").document(solicitud.idDoc)
                    .update("status", "rechazado") // Se marca como rechazado y desaparece de la lista
                    .addOnSuccessListener {
                        Toast.makeText(this, "Eskaera ukatua", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Utzi", null)
            .show()
    }
}