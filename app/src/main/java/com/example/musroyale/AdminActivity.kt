package com.example.musroyale

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musroyale.databinding.ActivityAdminPaymentsBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminPaymentsBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPaymentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar el RecyclerView inicialmente
        binding.rvSolicitudes.layoutManager = LinearLayoutManager(this)
        binding.btnBack.setOnClickListener {
            finish() // Esto cierra la actividad actual y vuelve a la anterior
        }
        cargarSolicitudes()
    }

    private fun cargarSolicitudes() {
        db.collection("SolicitudesRecarga")
            .whereEqualTo("status", "pendiente")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                // Solución al error de inferencia: Especificamos el tipo <SolicitudPago>
                val lista = snapshot?.documents?.mapNotNull { doc ->
                    SolicitudPago(
                        idDoc = doc.id,
                        userId = doc.getString("userId") ?: "",
                        username = doc.getString("username") ?: "",
                        monto = doc.getDouble("monto") ?: 0.0,
                        orderId = doc.getString("orderId") ?: ""
                    )
                } ?: listOf()

                // Dentro de cargarSolicitudes en AdminActivity
                val adapter = AdminPagosAdapter(lista) { solicitud ->
                    confirmarYVincular(solicitud)
                }
                binding.rvSolicitudes.adapter = adapter
            }
    }

    private fun confirmarYVincular(solicitud: SolicitudPago) {
        // 1. Sumar dinero al usuario (Asegúrate de que solicitud.userId sea el ID del documento en "Users")
        db.collection("Users").document(solicitud.userId)
            .update("dinero", FieldValue.increment(solicitud.monto))
            .addOnSuccessListener {
                // 2. Marcar como aprobado
                db.collection("SolicitudesRecarga").document(solicitud.idDoc)
                    .update("status", "aprobado")
                    .addOnSuccessListener {
                        Toast.makeText(this, "${solicitud.username}(r)en ordainketa onartua", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errorea saldoa kargatzean", Toast.LENGTH_SHORT).show()
            }
    }
}