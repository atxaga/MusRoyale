package com.example.musroyale

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.musroyale.databinding.ActivityPartidaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

class PartidaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPartidaBinding
    private val serverHost = "34.233.112.247"
    private val serverPort = 13000
    private val connectTimeoutMs = 20000
    private val currentCards = mutableListOf<String>()
    private val selectedIndices = mutableSetOf<Int>()
    private lateinit var bottomCard1: ImageView
    private lateinit var bottomCard2: ImageView
    private lateinit var bottomCard3: ImageView
    private lateinit var bottomCard4: ImageView
    private var ordagoOn: Boolean = false
    private var envidoOn: Boolean = false
    private var decisionContinuation: kotlinx.coroutines.CancellableContinuation<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPartidaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Usamos binding para acceder a las vistas directamente
        val txtUtzi = binding.txtPartidaUtzi

        binding.salir.setOnClickListener {
            if (txtUtzi.visibility == View.GONE) {
                txtUtzi.visibility = View.VISIBLE
                txtUtzi.postDelayed({
                    txtUtzi.visibility = View.GONE
                }, 3000)
            } else {
                txtUtzi.visibility = View.GONE
            }
        }

        binding.txtPartidaUtzi.setOnClickListener {
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

        // Inicializar vistas de cartas usando binding
        bottomCard1 = binding.bottomCard1
        bottomCard2 = binding.bottomCard2
        bottomCard3 = binding.bottomCard3
        bottomCard4 = binding.bottomCard4

        setupCardClick(bottomCard1, 0)
        setupCardClick(bottomCard2, 1)
        setupCardClick(bottomCard3, 2)
        setupCardClick(bottomCard4, 3)

        // Listeners de los botones usando binding
        binding.btnMus.setOnClickListener {
            decisionContinuation?.resume("mus", null)
        }
        binding.btnPasar.setOnClickListener {
            decisionContinuation?.resume("paso", null)
        }
        binding.btnDeskartea.setOnClickListener {
            val discardString = buildDiscardString()
            decisionContinuation?.resume(discardString, null)
        }
        binding.btnEnvido.setOnClickListener {
            decisionContinuation?.resume("2", null)
        }
        binding.btnQuiero.setOnClickListener {
            decisionContinuation?.resume("quiero", null)
        }
        binding.btnOrdago.setOnClickListener {
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

                var partidaActiva = true

                while (partidaActiva) {
                    binding.roundLabel.text = "BILATZEN..."

                    val serverMsg = reader.readLine() ?: break

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
                            binding.roundLabel.text = "BANATZEN"
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
                                binding.btnDeskartea.visibility = View.VISIBLE
                            }

                            val deskarteRespuesta = kotlinx.coroutines.suspendCancellableCoroutine<String> { cont ->
                                decisionContinuation = cont
                            }

                            writer.write(deskarteRespuesta)
                            writer.newLine()
                            writer.flush()

                            withContext(Dispatchers.Main) {
                                limpiarCartasDescartadas()
                                binding.btnDeskartea.visibility = View.GONE
                            }
                            recibirCartas(reader, 4)
                        }

                        serverMsg == "GRANDES" || serverMsg == "PEQUEÑAS" || serverMsg == "PARES" || serverMsg == "JUEGO" || serverMsg == "PUNTO" -> {
                            binding.roundLabel.text = serverMsg
                            withContext(Dispatchers.Main) {
                                toggleEnvidoButtons(visible = true)
                                Toast.makeText(this@PartidaActivity, "$serverMsg jolasten, zure txanda da!", Toast.LENGTH_SHORT).show()
                            }

                            val respuesta = kotlinx.coroutines.suspendCancellableCoroutine<String> { cont ->
                                decisionContinuation = cont
                            }

                            writer.write(respuesta)
                            writer.newLine()
                            writer.flush()

                            withContext(Dispatchers.Main) {
                                binding.btnEnvido.visibility = View.GONE
                                binding.btnQuiero.visibility = View.GONE
                                binding.btnPasar.visibility = View.GONE
                                binding.btnOrdago.visibility = View.GONE
                            }
                        }

                        serverMsg == "ORDAGO" -> {
                            ordagoOn = true
                        }
                        serverMsg == "ENVIDO" -> {
                            envidoOn = true
                        }
                        serverMsg == "PUNTUAKJASO" -> {
                            binding.roundLabel.text = "Puntuazioa"

                            withContext(Dispatchers.Main){
                                binding.leftScoreBox1.text = reader.readLine()
                                binding.leftScoreBox2.text = reader.readLine()
                                binding.rightScoreBox1.text = reader.readLine()
                                binding.rightScoreBox2.text = reader.readLine()
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

    private fun mostrarDecision(playerZnb: Int, mensaje: String) {
        val statusView = when (playerZnb) {
            0 -> binding.statusBottom
            1 -> binding.statusLeft
            2 -> binding.statusTop
            3 -> binding.statusRight
            else -> null
        }

        statusView?.let { tv ->
            tv.text = mensaje.uppercase()
            tv.visibility = View.VISIBLE

            if (mensaje.lowercase() == "mus") {
                tv.setTextColor(android.graphics.Color.parseColor("#FFEB3B"))
            } else {
                tv.setTextColor(android.graphics.Color.parseColor("#FF5252"))
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

            tv.postDelayed({
                tv.animate().alpha(0f).setDuration(500).withEndAction {
                    tv.visibility = View.GONE
                }
            }, 2500)
        }
    }

    private suspend fun recibirCartas(reader: BufferedReader, cantidad: Int) {
        currentCards.clear()
        val views = listOf(binding.bottomCard1, binding.bottomCard2, binding.bottomCard3, binding.bottomCard4)

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
        val views = listOf(binding.bottomCard1, binding.bottomCard2, binding.bottomCard3, binding.bottomCard4)
        for (index in selectedIndices) {
            views[index].setImageResource(0)
            views[index].alpha = 1f
        }
    }

    private fun toggleDecisionButtons(visible: Boolean) {
        val estado = if (visible) View.VISIBLE else View.GONE
        binding.btnMus.visibility = estado
        binding.btnPasar.visibility = estado
    }

    private fun toggleEnvidoButtons(visible: Boolean) {
        val estado = if (visible) View.VISIBLE else View.GONE
        binding.btnEnvido.visibility = if (ordagoOn) View.GONE else estado
        binding.btnPasar.visibility = estado
        binding.btnQuiero.visibility = if (envidoOn) estado else View.GONE
        binding.btnOrdago.visibility = estado
    }

    private fun buildDiscardString(): String {
        if (selectedIndices.isEmpty()) return "*"
        val seleccionadas = selectedIndices.map { currentCards[it] }
        return seleccionadas.joinToString("-") + "*"
    }
}