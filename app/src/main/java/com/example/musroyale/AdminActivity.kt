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

        binding.btnBack.setOnClickListener {
            finish()
        }

        cargarSolicitudes()
    }

    private fun cargarSolicitudes() {
        db.collection("SolicitudesRecarga")
            .whereEqualTo("status", "pendiente")
            .addSnapshotListener { snapshot, _ ->
                val lista = snapshot?.map { doc ->
                    SolicitudPago(
                        idDoc = doc.id,
                        userId = doc.getString("userId") ?: "",
                        username = doc.getString("username") ?: "",
                        monto = doc.getDouble("monto") ?: 0.00,
                        orderId = doc.getString("orderId") ?: ""
                    )
                } ?: listOf()

                binding.rvSolicitudes.layoutManager = LinearLayoutManager(this)
                binding.rvSolicitudes.adapter = AdminPagosAdapter(lista) { solicitud ->
                    confirmarYVincular(solicitud)
                }
            }
    }

    private fun confirmarYVincular(solicitud: SolicitudPago) {
        val userRef = db.collection("Users").document(solicitud.userId)

        // 1. Obtenemos el documento del usuario para leer su saldo actual (que es String)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val dineroActualString = document.getString("dinero") ?: "0.00"

                // Convertimos el String a Double para poder sumar
                val dineroActualDouble = dineroActualString.toDoubleOrNull() ?: 0.00
                val nuevoSaldo = dineroActualDouble + solicitud.monto

                // 2. Guardamos el nuevo saldo otra vez como String
                userRef.update("dinero", nuevoSaldo.toString())
                    .addOnSuccessListener {

                        // 3. Marcamos la solicitud como aprobada
                        db.collection("SolicitudesRecarga").document(solicitud.idDoc)
                            .update("status", "aprobado")

                        Toast.makeText(this, "${solicitud.username}(r)en ordainketa onartua", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Errorea saldoa kargatzean", Toast.LENGTH_SHORT).show()
        }
    }
}