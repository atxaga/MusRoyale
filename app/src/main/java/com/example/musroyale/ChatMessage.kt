package com.example.musroyale

data class ChatMessage(
    val text: String,
    val isMine: Boolean, // true = enviado por mi, false = recibido
    val timestamp: String
)