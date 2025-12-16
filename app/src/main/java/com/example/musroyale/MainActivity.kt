package com.example.musroyale

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.musroyale.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val modes = listOf(
            GameMode(R.drawable.partida_azkarra) {
                startActivity(Intent(this, MatchSetupActivity::class.java))
            },
            GameMode( R.drawable.parejak) {
                startActivity(Intent(this, DuoActivity::class.java))
            },
            GameMode(R.drawable.pribatua) {
                startActivity(Intent(this, PrivateMatchSetupActivity::class.java))
            }
        )

        val adapter = GameModeAdapter(modes)
        binding.modesRecycler.adapter = adapter
        binding.modesRecycler.setHasFixedSize(true)
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.modesRecycler)

        binding.modesRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            this,
            RecyclerView.HORIZONTAL,
            false
        )
        binding.modesRecycler.addItemDecoration(ModesSpacingDecoration(16))
    }
}