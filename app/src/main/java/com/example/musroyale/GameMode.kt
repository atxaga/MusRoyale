package com.example.musroyale

data class GameMode(
    val titleRes: Int,
    val subtitleRes: Int,
    val imageRes: Int,
    val onClick: () -> Unit
)

