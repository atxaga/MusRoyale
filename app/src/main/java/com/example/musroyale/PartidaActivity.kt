package com.example.musroyale

import android.os.Bundle
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
    fun partidaHasi(){
        val bottomCard1 = findViewById<android.widget.ImageView>(R.id.bottomCard1)
        lifecycleScope.launch(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(serverHost, serverPort), connectTimeoutMs)

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = socket.getOutputStream().bufferedWriter()

                var turno = 0
                repeat(4){
                    turno++;
                    val karta = reader.readLine()
                    currentCards.add(karta)
                    val resId = resources.getIdentifier(karta, "drawable", packageName)
                    if (turno == 1){
                        bottomCard1?.setImageResource(resId)
                    }else if (turno == 2) {
                        val bottomCard2 = findViewById<android.widget.ImageView>(R.id.bottomCard2)
                        bottomCard2?.setImageResource(resId)
                    }else if (turno == 3) {
                        val bottomCard3 = findViewById<android.widget.ImageView>(R.id.bottomCard3)
                        bottomCard3?.setImageResource(resId)
                    }else if (turno == 4) {
                        val bottomCard4 = findViewById<android.widget.ImageView>(R.id.bottomCard4)
                        bottomCard4?.setImageResource(resId)
                    }
                }

                //Lehenengo jokalariaren erabakia bidaltzen da
                val erabakia = "mus"
                writer.write("$erabakia\n")
                writer.flush()

                val aukeratuta = reader.readLine()
                if(aukeratuta == "mus"){
                    delay(10_000)

                    val discard = withContext(Dispatchers.Main) {
                        buildDiscardString()
                    }

                    // Enviar al servidor
                    writer.write("$discard\n")
                    writer.flush()

                }

            }catch (e: Exception) {
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
