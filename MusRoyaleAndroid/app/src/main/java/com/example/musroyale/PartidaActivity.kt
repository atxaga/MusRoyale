package com.example.musroyale

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

class PartidaActivity : AppCompatActivity() {
    private val serverHost = "34.233.112.247"
    private val serverPort = 13000
    private val connectTimeoutMs = 20000
    private val currentCards = mutableListOf<String>()
    private val selectedIndices = mutableSetOf<Int>()

    private lateinit var bottomCard1: ImageView
    private lateinit var bottomCard2: ImageView
    private lateinit var bottomCard3: ImageView
    private lateinit var bottomCard4: ImageView
    private lateinit var deskarteButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partida)

        // Listeners para iconos de chat y camara
        val btnChat = findViewById<ImageButton>(R.id.buttonChat)
        val btnCam = findViewById<ImageButton>(R.id.buttonCamara)

        bottomCard1 = findViewById(R.id.bottomCard1)
        bottomCard2 = findViewById(R.id.bottomCard2)
        bottomCard3 = findViewById(R.id.bottomCard3)
        bottomCard4 = findViewById(R.id.bottomCard4)

        // Inicial alphas y listeners de selección (toggle)
        bottomCard1.alpha = 1f
        bottomCard2.alpha = 1f
        bottomCard3.alpha = 1f
        bottomCard4.alpha = 1f

        setupCardClick(bottomCard1, 0)
        setupCardClick(bottomCard2, 1)
        setupCardClick(bottomCard3, 2)
        setupCardClick(bottomCard4, 3)


        btnCam?.setOnClickListener {
            Toast.makeText(this, "Abrir cámara (placeholder)", Toast.LENGTH_SHORT).show()
            // TODO: abrir cámara / compartir vídeo
        }
        partidaHasi()
    }
    private fun setupCardClick(view: ImageView, index: Int) {
        view.setOnClickListener {
            if (selectedIndices.contains(index)) {
                selectedIndices.remove(index)
                view.alpha = 1f
            } else {
                selectedIndices.add(index)
                view.alpha = 0.5f
            }
        }
    }
    fun partidaHasi() {
        lifecycleScope.launch(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                // Conectar al servidor
                socket = Socket()
                socket.connect(InetSocketAddress(serverHost, serverPort), connectTimeoutMs)

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = socket.getOutputStream().bufferedWriter()

                var turno = 0
                var partidaActiva = true

                while (partidaActiva) {
                    val serverMsg = reader.readLine() ?: break // cliente desconectado
                    when (serverMsg) {
                        "CARDS" -> {
                            // Recibir las 4 cartas iniciales
                            repeat(4) { i ->
                                val karta = reader.readLine() ?: return@launch
                                currentCards.add(karta)
                                // Actualizar UI en el hilo principal
                                withContext(Dispatchers.Main) {
                                    val resId = resources.getIdentifier(karta, "drawable", packageName)
                                    when(i){
                                        0 -> bottomCard1.setImageResource(resId)
                                        1 -> bottomCard2.setImageResource(resId)
                                        2 -> bottomCard3.setImageResource(resId)
                                        3 -> bottomCard4.setImageResource(resId)
                                    }
                                }
                            }
                        }

                        "TURN" -> {
                            // Es tu turno de decidir
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@PartidaActivity, "Zure Txanda da", Toast.LENGTH_LONG).show()
                            }
                            // Por ahora siempre hacemos "mus" (puedes cambiar según UI)
                            val erabakia = "mus"
                            writer.write(erabakia)
                            writer.newLine()
                            writer.flush()
                        }

                        "ALL_MUS" -> {
                            // Mostrar botón en el hilo principal
                            withContext(Dispatchers.Main) {
                                deskarteButton.visibility = Button.VISIBLE

                                deskarteButton.setOnClickListener {
                                    // Ejecutar la escritura en IO
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val discard = buildDiscardString()
                                        writer.write(discard)
                                        writer.newLine()
                                        writer.flush()

                                        // Ocultar botón en hilo principal
                                        withContext(Dispatchers.Main) {
                                            deskarteButton.visibility = Button.GONE
                                        }
                                    }
                                }
                            }
                        }

                        "END_GAME" -> {
                            // Fin de la partida
                            partidaActiva = false
                        }

                        else -> {
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PartidaActivity, "Error TCP: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }
    }

    private fun buildDiscardString(): String {
        if (selectedIndices.isEmpty()) return "*" // solo marcador final
        val ordered = selectedIndices.sorted()
        val parts = ordered.mapNotNull { idx -> currentCards.getOrNull(idx) }
        return parts.joinToString("-") + "*"
    }
}
