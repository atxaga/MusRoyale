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
            GameMode(R.string.mode_quick_match, R.string.mode_quick_match_desc, R.drawable.rapida) {
                startActivity(Intent(this, MatchSetupActivity::class.java))
            },
            GameMode(R.string.mode_duo, R.string.mode_duo_desc, R.drawable.duo) {
                startActivity(Intent(this, DuoActivity::class.java))
            },
            GameMode(R.string.mode_private, R.string.mode_private_desc, R.drawable.privada) {
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