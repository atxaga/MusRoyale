package com.example.musroyale

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.musroyale.databinding.ActivityDuosBinding
import com.example.musroyale.databinding.ActivityMainBinding
import com.example.musroyale.databinding.ActivityPribatuaBinding

class PrivateMatchSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPribatuaBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPribatuaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            var intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        binding.btnJoin.setOnClickListener {
            val valueToSend = "UNIRSE_PRIVADA"
            val kodea = binding.codeInput.text.toString()
            val intent = Intent(this, PartidaActivity::class.java)
            intent.putExtra(PartidaActivity.EXTRA_PARAM, valueToSend)
            intent.putExtra(PartidaActivity.EXTRA_CODE, kodea)
            startActivity(intent)
        }
        binding.btnCreate.setOnClickListener {
            val valueToSend = "CREAR_PRIVADA"
            val intent = Intent(this, PartidaActivity::class.java)
            intent.putExtra(PartidaActivity.EXTRA_PARAM, valueToSend)
            startActivity(intent)
        }
        // Mostrar SortuFragment


        /*supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SartuFragment())
            .commit()*/




    }
}