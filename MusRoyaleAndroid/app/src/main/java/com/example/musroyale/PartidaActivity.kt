package com.example.musroyale

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import android.widget.TextView

class PartidaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PARAM = "com.example.musroyale.EXTRA_PARAM"
        const val EXTRA_CODE = "com.example.musroyale.EXTRA_CODE"
    }

    private val serverHost = "34.233.112.247"
    private val serverPort = 13000
    private val connectTimeoutMs = 20000
    private val currentCards = mutableListOf<String>()
    private val selectedIndices = mutableSetOf<Int>()

    private lateinit var bottomCard1: ImageView
    private lateinit var bottomCard2: ImageView
    private lateinit var bottomCard3: ImageView
    private lateinit var bottomCard4: ImageView
    private lateinit var roundLabel: TextView

    private var ordagoOn: Boolean = false
    private var envidoOn: Boolean = false
    private var decisionContinuation: kotlinx.coroutines.CancellableContinuation<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Establecer el layout directamente
        setContentView(R.layout.activity_partida)

        val iconoAjustes = findViewById<ImageView>(R.id.salir)
        val txtUtzi = findViewById<TextView>(R.id.txtPartidaUtzi)
        roundLabel = findViewById(R.id.roundLabel)

        iconoAjustes.setOnClickListener {
            if (txtUtzi.visibility == View.GONE) {
                txtUtzi.visibility = View.VISIBLE
                txtUtzi.postDelayed({
                    txtUtzi.visibility = View.GONE
                }, 3000)
            } else {
                txtUtzi.visibility = View.GONE
            }
        }

        txtUtzi.setOnClickListener {
            val view = layoutInflater.inflate(R.layout.dialog_custom_exit, null)
            val dialog = android.app.Dialog(this)
            dialog.setContentView(view)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val btnSi = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSi)
            val btnNo = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNo)

            btnSi.setOnClickListener {
                dialog.dismiss()
                finish()
            }

            btnNo.setOnClickListener {
                dialog.dismiss()
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

        // --- LISTENERS DE LOS BOTONES CON R.ID ---
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
        findViewById<Button>(R.id.btnOrdago).setOnClickListener {
            decisionContinuation?.resume("ordago", null)
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

                val received = intent.getStringExtra(EXTRA_PARAM)
                writer.write(received)
                writer.newLine()
                writer.flush()

                withContext(Dispatchers.Main) {
                    roundLabel.text = "BILATZEN..."
                }
                while (true) {

                    val serverMsg = reader.readLine() ?: break

                    when {
                        serverMsg.startsWith("ACTION:") -> {
                            val partes = serverMsg.split(":")
                            if (partes.size >= 3) {
                                withContext(Dispatchers.Main) {
                                    runOnUiThread { mostrarDecision(partes[1].toInt(), partes[2]) }
                                }
                            }
                        }

                        serverMsg == "CARDS" -> {
                            withContext(Dispatchers.Main) {
                                roundLabel.text = "BANATZEN"
                            }
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

                        // Agrupamos las fases de juego
                        serverMsg == "GRANDES" || serverMsg == "PEQUEÑAS" || serverMsg == "PARES" || serverMsg == "JUEGO" || serverMsg == "PUNTO" -> {
                            withContext(Dispatchers.Main) {
                                roundLabel.text = serverMsg
                                toggleEnvidoButtons(visible = true)
                                Toast.makeText(this@PartidaActivity, "$serverMsg jolasten!", Toast.LENGTH_SHORT).show()
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
                                findViewById<Button>(R.id.btnOrdago).visibility = View.GONE
                            }
                            ordagoOn = false
                            envidoOn = false
                        }

                        serverMsg == "ORDAGO" -> ordagoOn = true
                        serverMsg == "ENVIDO" -> envidoOn = true

                        serverMsg == "PUNTUAKJASO" -> {
                            val left1 = reader.readLine()
                            val left2 = reader.readLine()
                            val right1 = reader.readLine()
                            val right2 = reader.readLine()
                            withContext(Dispatchers.Main) {
                                roundLabel.text = "Puntuazioa"
                                findViewById<TextView>(R.id.leftScoreBox1).text = left1
                                findViewById<TextView>(R.id.leftScoreBox2).text = left2
                                findViewById<TextView>(R.id.rightScoreBox1).text = right1
                                findViewById<TextView>(R.id.rightScoreBox2).text = right2
                            }
                        }
                        serverMsg == "PEDIR_CODIGO" -> {
                            val kodea = intent.getStringExtra(EXTRA_CODE) ?: ""
                            writer.write(kodea)
                            writer.newLine()
                            writer.flush()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PartidaActivity, "Konexio errorea: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("PartidaActivity", e.message, e)
                }
            } finally {
                socket?.close()
            }
        }
    }

    private fun mostrarDecision(playerZnb: Int, mensaje: String) {
        val statusId = when (playerZnb) {
            0 -> R.id.statusBottom
            1 -> R.id.statusLeft
            2 -> R.id.statusTop
            3 -> R.id.statusRight
            else -> null
        }

        statusId?.let { id ->
            val tv = findViewById<TextView>(id)
            tv.text = mensaje.uppercase()
            tv.visibility = View.VISIBLE

            when (mensaje.lowercase()) {
                "mus" -> tv.setTextColor(android.graphics.Color.parseColor("#FFEB3B")) // Amarillo
                "envido", "2" -> tv.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Verde
                "ordago" -> tv.setTextColor(android.graphics.Color.parseColor("#FF5252")) // Rojo
                else -> tv.setTextColor(android.graphics.Color.WHITE)
            }

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

            // 4. Desvanecer después de 3 segundos
            tv.removeCallbacks(null) // Limpiar callbacks previos si el jugador habla rápido
            tv.postDelayed({
                tv.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(400)
                    .withEndAction {
                        tv.visibility = View.GONE
                    }
            }, 3000)
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
                views[i].visibility = View.VISIBLE
                views[i].alpha = 1f
            }
        }

        withContext(Dispatchers.Main) {
            selectedIndices.clear()
        }
    }

    private fun limpiarCartasDescartadas() {
        val views = listOf(bottomCard1, bottomCard2, bottomCard3, bottomCard4)
        for (index in selectedIndices) {
            views[index].setImageResource(0)
            views[index].alpha = 1f
        }
    }

    private fun toggleDecisionButtons(visible: Boolean) {
        val estado = if (visible) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnMus).visibility = estado
        findViewById<Button>(R.id.btnPasar).visibility = estado
    }

    private fun toggleEnvidoButtons(visible: Boolean) {
        val estado = if (visible) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnEnvido).visibility = if (ordagoOn) View.GONE else estado
        findViewById<Button>(R.id.btnPasar).visibility = estado
        findViewById<Button>(R.id.btnQuiero).visibility = if (envidoOn || ordagoOn) estado else View.GONE
        findViewById<Button>(R.id.btnOrdago).visibility = if (ordagoOn) View.GONE else estado
    }

    private fun buildDiscardString(): String {
        if (selectedIndices.isEmpty()) return "*"
        val seleccionadas = selectedIndices.map { currentCards[it] }
        return seleccionadas.joinToString("-") + "*"
    }
}