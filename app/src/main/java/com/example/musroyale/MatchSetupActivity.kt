package com.example.musroyale

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.snackbar.Snackbar

class MatchSetupActivity : AppCompatActivity() {

    private var selectedBetButton: MaterialButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_match_setup)

        val toggleGroupMatchType: MaterialButtonToggleGroup = findViewById(R.id.toggleGroupMatchType)
        val toggleGroupReyes: MaterialButtonToggleGroup = findViewById(R.id.toggleGroupReyes)
        val btnPlay: Button = findViewById(R.id.btnPlay)

        val betButtons = listOf(
            findViewById<MaterialButton>(R.id.btnBet100),
            findViewById<MaterialButton>(R.id.btnBet250),
            findViewById<MaterialButton>(R.id.btnBet500),
            findViewById<MaterialButton>(R.id.btnBet1000),
            findViewById<MaterialButton>(R.id.btnBet2500),
            findViewById<MaterialButton>(R.id.btnBet5000)
        )

        toggleGroupMatchType.check(R.id.btnMatchOnline)
        toggleGroupReyes.check(R.id.btnFourKings)
        selectBetButton(betButtons[1]) // 250€ por defecto

        betButtons.forEach { button ->
            button.setOnClickListener {
                selectBetButton(button)
            }
        }

        btnPlay.setOnClickListener {
            val matchType = when (toggleGroupMatchType.checkedButtonId) {
                R.id.btnMatchIA -> getString(R.string.match_ia)
                else -> getString(R.string.match_online)
            }
            val selectedMode = when (toggleGroupReyes.checkedButtonId) {
                R.id.btnEightKings -> getString(R.string.eight_kings)
                else -> getString(R.string.four_kings)
            }
            val selectedBet = selectedBetButton?.text?.toString() ?: getString(R.string.match_bet_250)
            val message = getString(R.string.match_summary_template, matchType, selectedMode, selectedBet)
            Snackbar.make(btnPlay, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun selectBetButton(button: MaterialButton) {
        selectedBetButton?.let {
            it.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.toggle_bg_unselected))
            it.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.toggle_stroke_unselected))
            it.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.toggle_bg_selected))
        button.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.toggle_stroke_selected))
        button.setTextColor(ContextCompat.getColor(this, R.color.toggle_text_selected))
        selectedBetButton = button
    }
}
