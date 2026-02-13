package com.example.musroyale

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

class IAMusMotor(apiKey: String) {
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
        // Mejoramos el prompt para que Gemini entienda la jerarquía del Mus
        val prompt = """
            Eres un experto jugador de Mus (reglas españolas). 
            VALOR DE CARTAS: 
            - Reyes y 3s: Valen 10 (las mejores para 'Grande').
            - Ases y 2s: Valen 1 (las mejores para 'Chica').
            - Figuras (Sota=8, Caballo=9, Rey=10).
            
            ESTADO DEL JUEGO:
            - Fase actual: $fase
            - Mi mano: ${misCartas.joinToString(", ")}
            - Historial de apuestas: $historial
            - Apuesta actual en mesa: $puntosEnMesa puntos.
            
            REGLAS DE RESPUESTA:
            1. Si la fase es 'GRANDES' y tienes muchos Reyes/3s, sube la apuesta ('envido').
            2. Si la fase es 'PEQUEÑAS' y tienes muchos Ases/2s, sube la apuesta ('envido').
            3. Si la apuesta es muy alta y tu mano es mediocre para la fase, di 'paso'.
            4. Si tienes una mano imbatible (ej. 4 Reyes o 4 Ases), di 'ordago'.
            5. Si te han envidado y tienes algo razonable, di 'quiero'.
            6. Si la fase es de descarte y no tienes nada, di 'mus'.
            
            Responde ÚNICAMENTE con una de estas palabras: [envido, paso, quiero, ordago, mus].
            
            Decisión:
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            val decision = response.text?.trim()?.lowercase() ?: "paso"

            // Filtro de seguridad para asegurar que solo devuelve palabras permitidas
            val opcionesValidas = listOf("envido", "paso", "quiero", "ordago", "mus")
            if (decision in opcionesValidas) decision else "paso"
        } catch (e: Exception) {
            "paso"
        }
    }
}