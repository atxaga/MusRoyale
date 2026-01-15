package com.example.musroyale

data class SolicitudPago(
    val idDoc: String,
    val userId: String,
    val username: String,
    val monto: Double,
    val orderId: String,
    val status: String // <--- Asegúrate de añadir esto
)