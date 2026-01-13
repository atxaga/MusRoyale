package com.example.musroyale

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musroyale.databinding.ActivityAdminPaymentsBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.creativityapps.gmailbackgroundlibrary.BackgroundMail // Asegúrate de tener la librería

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminPaymentsBinding
    private val db = FirebaseFirestore.getInstance()

    // Configura tus credenciales aquí una sola vez
    private val MI_CORREO_ADMIN = "musroyale@gmail.com"
    private val MI_PASSWORD_APP = "tshrtafagmostgdn" // Tus 16 letras de Google

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPaymentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
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
                    Toast.makeText(this, "Errorea: Saldo nahikorik ez", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val saldoFormateado = String.format(java.util.Locale.US, "%.2f", nuevoSaldo)

                userRef.update("dinero", saldoFormateado)
                    .addOnSuccessListener {
                        // Actualizar estado en Firebase
                        db.collection("SolicitudesRecarga").document(solicitud.idDoc)
                            .update("status", "aprobado")

                        // ENVIAR EMAIL DE APROBACIÓN
                        obtenerEmailYEnviar(solicitud.userId, "Aprobada", solicitud.monto.toString())

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
            .setPositiveButton("Bai, Ezeztatu") { _, _ ->
                db.collection("SolicitudesRecarga").document(solicitud.idDoc)
                    .update("status", "rechazado")
                    .addOnSuccessListener {
                        // ENVIAR EMAIL DE RECHAZO
                        obtenerEmailYEnviar(solicitud.userId, "Rechazada", solicitud.monto.toString())
                        Toast.makeText(this, "Eskaera ukatua", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Utzi", null)
            .show()
    }

    // --- FUNCIONES DE EMAIL ---

    private fun obtenerEmailYEnviar(userId: String, estado: String, monto: String) {
        db.collection("Users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val emailDestino = userDoc.getString("email")
                if (emailDestino != null) {
                    enviarEmailSegundoPlano(emailDestino, estado, monto)
                }
            }
    }

    private fun enviarEmailSegundoPlano(emailUsuario: String, estado: String, monto: String) {
        val passLimpia = MI_PASSWORD_APP.replace(" ", "")

        Thread {
            try {
                BackgroundMail.newBuilder(this)
                    .withUsername(MI_CORREO_ADMIN)
                    .withPassword(passLimpia)
                    .withMailto(emailUsuario)
                    .withType(BackgroundMail.TYPE_PLAIN)
                    .withSubject("Actualización MusRoyale")
                    .withBody("Tu solicitud de $monto USDT ha sido $estado.")
                    .withProcessVisibility(false)
                    .withOnSuccessCallback {
                        runOnUiThread {
                            Toast.makeText(this, "¡Correo enviado!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .withOnFailCallback {
                        // Quitamos el "e ->" porque la librería no lo soporta aquí
                        runOnUiThread {
                            Toast.makeText(this, "Error al enviar correo. Revisa logs.", Toast.LENGTH_LONG).show()
                        }
                    }
                    .send()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}