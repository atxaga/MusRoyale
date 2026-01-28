package com.example.musroyale

data class ChatMessage(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isMine: Boolean = false,
    val leido: Boolean = false
)