package com.example.musroyale

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

class IAMusMotor(apiKey: String) {
    // Usamos flash porque es más rápido para respuestas cortas de juego
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    suspend fun obtenerDecision(
        fase: String,
        misCartas: List<String>,
        historial: String,
        puntosEnMesa: Int
    ): String {
        val prompt = """
            Eres un jugador profesional de Mus. 
            Reglas estrictas: Responde SOLO con una palabra en minúsculas.
            
            Contexto actual:
            - Fase de la ronda: $fase
            - Mis cartas: ${misCartas.joinToString(", ")}
            - Historial de esta ronda: $historial
            - Puntos apostados actualmente: $puntosEnMesa
            
            Opciones permitidas: [envido, paso, quiero, ordago, mus]
            
            Tu decisión técnica:
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            response.text?.trim()?.lowercase() ?: "paso"
        } catch (e: Exception) {
            "paso" // Fallback por seguridad
        }
    }
}