package com.example.musroyale

// Modelo simple para representar un amigo/sala en el chat
data class Friend(
    val id: String,
    val name: String,
    var unreadCount: Int = 0,
    val avatar: String = "avadef.png" // Añadido
)
