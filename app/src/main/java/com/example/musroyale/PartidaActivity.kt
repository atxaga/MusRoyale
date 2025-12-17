package com.example.musroyale

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PartidaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partida)

        // Listeners para iconos de chat y camara
        val btnChat = findViewById<ImageButton>(R.id.buttonChat)
        val btnCam = findViewById<ImageButton>(R.id.buttonCamara)

        btnChat?.setOnClickListener {
            val chatDialog = ChatDialogFragment()
            chatDialog.show(supportFragmentManager, "ChatPopup")
        }


        btnCam?.setOnClickListener {
            Toast.makeText(this, "Abrir cámara (placeholder)", Toast.LENGTH_SHORT).show()
            // TODO: abrir cámara / compartir vídeo
        }
    }
}
