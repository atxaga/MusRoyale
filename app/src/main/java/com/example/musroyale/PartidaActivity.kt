package com.example.musroyale

import android.os.Bundle
import android.widget.ImageButton
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

class PartidaActivity : AppCompatActivity() {
    private val serverHost = "10.14.0.106"
    private val serverPort = 13000
    private val connectTimeoutMs = 20000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partida)

        // Listeners para iconos de chat y camara
        val btnChat = findViewById<ImageButton>(R.id.buttonChat)
        val btnCam = findViewById<ImageButton>(R.id.buttonCamara)




        btnCam?.setOnClickListener {
            Toast.makeText(this, "Abrir cámara (placeholder)", Toast.LENGTH_SHORT).show()
            // TODO: abrir cámara / compartir vídeo
        }
        partidaHasi()
    }
    fun partidaHasi(){
        val bottomCard1 = findViewById<android.widget.ImageView>(R.id.bottomCard1)
        lifecycleScope.launch(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(serverHost, serverPort), connectTimeoutMs)

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                var turno = 0
                repeat(4){
                    turno++;
                    val karta = reader.readLine()
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

            }catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PartidaActivity, "Error TCP: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }
    }
}
