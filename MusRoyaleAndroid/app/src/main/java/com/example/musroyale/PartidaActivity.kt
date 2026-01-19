package com.example.musroyale

import android.os.Bundle
import android.view.View
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
import android.widget.TextView

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
    private lateinit var headerPartidaFragment: HeaderPartidaFragment
    // Referencia para pausar/reanudar la lectura del servidor
    private var decisionContinuation: kotlinx.coroutines.CancellableContinuation<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partida)
        val iconoAjustes = findViewById<ImageView>(R.id.salir)

        val txtUtzi = findViewById<TextView>(R.id.txtPartidaUtzi)

        iconoAjustes.setOnClickListener {
            // Si está oculto lo muestra, si está visible lo oculta
            if (txtUtzi.visibility == View.GONE) {
                txtUtzi.visibility = View.VISIBLE

                // Opcional: Que se oculte solo después de 3 segundos
                txtUtzi.postDelayed({
                    txtUtzi.visibility = View.GONE
                }, 3000)
            } else {
                txtUtzi.visibility = View.GONE
            }
        }

        txtUtzi.setOnClickListener {
            // Inflar el layout personalizado
            val view = layoutInflater.inflate(R.layout.dialog_custom_exit, null)

            val dialog = android.app.Dialog(this)
            dialog.setContentView(view)

            // Hacer el fondo transparente para que se vean las esquinas redondeadas del CardView
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val btnSi = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSi)
            val btnNo = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNo)

            btnSi.setOnClickListener {
                dialog.dismiss()
                finish() // Salir de la partida
            }

            btnNo.setOnClickListener {
                dialog.dismiss() // Cerrar el diálogo y seguir jugando
            }

            dialog.show()
        }
        // Inicializar vistas de cartas
        bottomCard1 = findViewById(R.id.bottomCard1)
        bottomCard2 = findViewById(R.id.bottomCard2)
        bottomCard3 = findViewById(R.id.bottomCard3)
        bottomCard4 = findViewById(R.id.bottomCard4)

        setupCardClick(bottomCard1, 0)
        setupCardClick(bottomCard2, 1)
        setupCardClick(bottomCard3, 2)
        setupCardClick(bottomCard4, 3)

        // --- LISTENERS DE LOS BOTONES (FUERA DEL BUCLE) ---
        findViewById<Button>(R.id.btnMus).setOnClickListener {
            decisionContinuation?.resume("mus", null)
        }
        findViewById<Button>(R.id.btnPasar).setOnClickListener {
            decisionContinuation?.resume("paso", null)
        }
        findViewById<Button>(R.id.btnDeskartea).setOnClickListener {
            val discardString = buildDiscardString()
            decisionContinuation?.resume(discardString, null)
        }
        findViewById<Button>(R.id.btnEnvido).setOnClickListener {
            decisionContinuation?.resume("2", null)
        }
        findViewById<Button>(R.id.btnQuiero).setOnClickListener {
            decisionContinuation?.resume("quiero", null)
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
                socket = Socket()
                socket.connect(InetSocketAddress(serverHost, serverPort), connectTimeoutMs)

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = socket.getOutputStream().bufferedWriter()

                var partidaActiva = true

                while (partidaActiva) {
                    val serverMsg = reader.readLine() ?: break

                    // CORRECCIÓN: Usamos when sin argumento para poder usar serverMsg.startsWith
                    when {
                        serverMsg.startsWith("DECISION:") -> {
                            val partes = serverMsg.split(":")
                            if (partes.size >= 3) {
                                val pId = partes[1].toInt()
                                val decision = partes[2]
                                withContext(Dispatchers.Main) {
                                    mostrarDecision(pId, decision)
                                }
                            }
                        }

                        serverMsg == "CARDS" -> {
                            recibirCartas(reader, 4)
                        }

                        serverMsg == "TURN" -> {
                            withContext(Dispatchers.Main) {
                                toggleDecisionButtons(visible = true)
                                Toast.makeText(this@PartidaActivity, "Zure txanda!", Toast.LENGTH_SHORT).show()
                            }

                            val respuesta = kotlinx.coroutines.suspendCancellableCoroutine<String> { cont ->
                                decisionContinuation = cont
                            }

                            writer.write(respuesta)
                            writer.newLine()
                            writer.flush()

                            withContext(Dispatchers.Main) {
                                toggleDecisionButtons(visible = false)
                            }
                        }

                        serverMsg == "ALL_MUS" -> {
                            withContext(Dispatchers.Main) {
                                findViewById<Button>(R.id.btnDeskartea).visibility = View.VISIBLE
                            }

                            val deskarteRespuesta = kotlinx.coroutines.suspendCancellableCoroutine<String> { cont ->
                                decisionContinuation = cont
                            }

                            writer.write(deskarteRespuesta)
                            writer.newLine()
                            writer.flush()

                            withContext(Dispatchers.Main) {
                                limpiarCartasDescartadas()
                                findViewById<Button>(R.id.btnDeskartea).visibility = View.GONE
                            }
                            recibirCartas(reader, 4)
                        }

                        serverMsg == "GRANDES" -> {
                            withContext(Dispatchers.Main) {
                                toggleEnvidoButtons(visible = true)
                                Toast.makeText(this@PartidaActivity, "Grandes jolasten, zure txanda da!", Toast.LENGTH_SHORT).show()
                            }

                            val respuesta = kotlinx.coroutines.suspendCancellableCoroutine<String> { cont ->
                                decisionContinuation = cont
                            }

                            writer.write(respuesta)
                            writer.newLine()
                            writer.flush()

                            withContext(Dispatchers.Main) {
                                findViewById<Button>(R.id.btnEnvido).visibility = View.GONE
                                findViewById<Button>(R.id.btnQuiero).visibility = View.GONE
                                findViewById<Button>(R.id.btnPasar).visibility = View.GONE
                            }
                        }
                        serverMsg == "PUNTUAKJASO" -> {
                            withContext(Dispatchers.Main){
                                val ezkerrekoTaldea1 = reader.readLine()
                                val ezkerrekoTaldea2 = reader.readLine()
                                headerPartidaFragment.setLeftScoreBoxes(ezkerrekoTaldea1, ezkerrekoTaldea2)

                                val eskuinekoTaldea1 = reader.readLine()
                                val eskuinekoTaldea2 = reader.readLine()
                                headerPartidaFragment.setRightScoreBoxes(eskuinekoTaldea1, eskuinekoTaldea2)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PartidaActivity, "Konexio errorea: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                socket?.close()
            }
        }
    }

    // FUNCIÓN DE DECISIÓN MEJORADA (MÁS BONITA Y CON ANIMACIÓN)
    private fun mostrarDecision(playerZnb: Int, mensaje: String) {
        val statusView = when (playerZnb) {
            0 -> findViewById<android.widget.TextView>(R.id.statusBottom)
            1 -> findViewById<android.widget.TextView>(R.id.statusLeft)
            2 -> findViewById<android.widget.TextView>(R.id.statusTop)
            3 -> findViewById<android.widget.TextView>(R.id.statusRight)
            else -> null
        }

        statusView?.let { tv ->
            tv.text = mensaje.uppercase()
            tv.visibility = View.VISIBLE

            // Estilo visual dinámico
            if (mensaje.lowercase() == "mus") {
                tv.setTextColor(android.graphics.Color.parseColor("#FFEB3B")) // Amarillo Mus
            } else {
                tv.setTextColor(android.graphics.Color.parseColor("#FF5252")) // Rojo Paso
            }

            // Animación de aparición (Pop-up)
            tv.alpha = 0f
            tv.scaleX = 0.5f
            tv.scaleY = 0.5f
            tv.animate()
                .alpha(1f)
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(300)
                .withEndAction {
                    tv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100)
                }

            // Desvanecer y ocultar tras 2.5 segundos
            tv.postDelayed({
                tv.animate().alpha(0f).setDuration(500).withEndAction {
                    tv.visibility = View.GONE
                }
            }, 2500)
        }
    }

    private suspend fun recibirCartas(reader: BufferedReader, cantidad: Int) {
        currentCards.clear()
        val views = listOf(bottomCard1, bottomCard2, bottomCard3, bottomCard4)

        repeat(cantidad) { i ->
            val karta = reader.readLine() ?: return@repeat
            currentCards.add(karta)

            withContext(Dispatchers.Main) {
                val resId = resources.getIdentifier(karta, "drawable", packageName)
                views[i].setImageResource(resId)
                views[i].visibility = View.VISIBLE // Por si acaso estaban ocultas
                views[i].alpha = 1f
            }
        }

        // Limpiamos los índices seleccionados para la siguiente ronda
        withContext(Dispatchers.Main) {
            selectedIndices.clear()
        }
    }
    private fun limpiarCartasDescartadas() {
        val views = listOf(bottomCard1, bottomCard2, bottomCard3, bottomCard4)

        // Para cada índice seleccionado (0, 1, 2 o 3)
        for (index in selectedIndices) {
            views[index].setImageResource(0) // Borra la imagen de la carta
            views[index].alpha = 1f          // Reseteamos el alpha para cuando llegue la nueva
        }

        // IMPORTANTE: No borres selectedIndices aquí,
        // se borran dentro de resetCardSelection() después de recibir las nuevas.
    }
    private fun toggleDecisionButtons(visible: Boolean) {
        val estado = if (visible) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<Button>(R.id.btnMus).visibility = estado
        findViewById<Button>(R.id.btnPasar).visibility = estado
    }
    private fun toggleEnvidoButtons(visible: Boolean) {
        val estado = if (visible) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<Button>(R.id.btnEnvido).visibility = estado
        findViewById<Button>(R.id.btnPasar).visibility = estado
        findViewById<Button>(R.id.btnQuiero).visibility = estado
    }



    private fun resetCardSelection() {
        selectedIndices.clear()
        listOf(bottomCard1, bottomCard2, bottomCard3, bottomCard4).forEach { it.alpha = 1f }
    }
    private fun buildDiscardString(): String {
        // Si no hay cartas seleccionadas, enviamos solo el asterisco (o vacío según C#)
        if (selectedIndices.isEmpty()) return "*"

        // Cogemos los nombres de las cartas seleccionadas
        val seleccionadas = selectedIndices.map { currentCards[it] }

        // Resultado: "oro1-copa12*"
        return seleccionadas.joinToString("-") + "*"
    }
}
