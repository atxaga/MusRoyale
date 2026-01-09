package com.example.musroyale

data class SolicitudPago(
    val idDoc: String = "",
    val userId: String = "",
    val username: String = "",
    val monto: Double = 0.0,
    val orderId: String = ""
)