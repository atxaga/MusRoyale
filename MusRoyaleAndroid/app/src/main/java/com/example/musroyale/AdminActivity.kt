package com.example.musroyale

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.musroyale.databinding.ActivityAdminPaymentsBinding
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminPaymentsBinding
    private val db = FirebaseFirestore.getInstance()

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
                val nuevoSaldo = if (solicitud.status == "retirada") dineroActualDouble - solicitud.monto else dineroActualDouble + solicitud.monto

                if (nuevoSaldo < 0 && solicitud.status == "retirada") {
                    Toast.makeText(this, "Errorea: Saldo nahikorik ez", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val saldoFormateado = String.format(java.util.Locale.US, "%.2f", nuevoSaldo)
                userRef.update("dinero", saldoFormateado).addOnSuccessListener {
                    db.collection("SolicitudesRecarga").document(solicitud.idDoc).update("status", "aprobado")
                    obtenerEmailYEnviar(solicitud.userId, "Aprobada", solicitud.monto.toString())
                    Toast.makeText(this, "Karga onartua: ${solicitud.username}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun rechazarSolicitud(solicitud: SolicitudPago) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Eskaera Ukatu")
            .setMessage("Ziur zaude ${solicitud.username}(r)en eskaera ukatu nahi duzula?")
            .setPositiveButton("Bai, Ezeztatu") { _, _ ->
                db.collection("SolicitudesRecarga").document(solicitud.idDoc).update("status", "rechazado")
                    .addOnSuccessListener {
                        obtenerEmailYEnviar(solicitud.userId, "Rechazada", solicitud.monto.toString())
                        Toast.makeText(this, "Eskaera ukatua", Toast.LENGTH_SHORT).show()
                    }
            }.setNegativeButton("Utzi", null).show()
    }

    private fun obtenerEmailYEnviar(userId: String, estado: String, monto: String) {
        db.collection("Users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val emailDestino = userDoc.getString("email")
                if (emailDestino != null) {

                    // Definimos el texto y el color según el estado
                    val (textoEuskera, colorHex) = if (estado == "Aprobada") {
                        "ONARTUA" to "#2e7d32" // Verde
                    } else {
                        "EZEZTATUA" to "#d32f2f"  // Rojo
                    }

                    // Llamamos a la función enviando el nuevo parámetro de color
                    enviarEmailConEmailJS(emailDestino, textoEuskera, monto, colorHex)
                }
            }
    }

    // Actualiza la firma de la función para recibir el color
    private fun enviarEmailConEmailJS(emailUsuario: String, estado: String, monto: String, color: String) {
        val url = "https://api.emailjs.com/api/v1.0/email/send"

        val jsonBody = JSONObject()
        jsonBody.put("service_id", "service_uher0p5")
        jsonBody.put("template_id", "template_vmizwuo")
        jsonBody.put("user_id", "NKnaCzLatjnCP9V5w")

        val templateParams = JSONObject()
        templateParams.put("to_email", emailUsuario)
        templateParams.put("status", estado)
        templateParams.put("amount", monto)
        templateParams.put("color", color) // <--- ENVIAMOS EL COLOR AQUÍ

        jsonBody.put("template_params", templateParams)

        // ... resto del código de Volley igual ...
        val queue = Volley.newRequestQueue(this)
        val stringRequest = object : com.android.volley.toolbox.StringRequest(
            Method.POST, url,
            { Log.d("EMAILJS", "Bidalia!") },
            { error -> Log.e("EMAILJS", "Errorea") }
        ) {
            override fun getBodyContentType(): String = "application/json; charset=utf-8"
            override fun getBody(): ByteArray = jsonBody.toString().toByteArray()
        }
        queue.add(stringRequest)
    }
}