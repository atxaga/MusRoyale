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
        // Mostrar SortuFragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SortuFragment())
            .commit()

        /*supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SartuFragment())
            .commit()*/




    }
}