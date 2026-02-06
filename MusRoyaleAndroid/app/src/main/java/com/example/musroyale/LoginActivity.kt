package com.example.musroyale

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {
    private lateinit var etUsuario: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var loadingOverlay: View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        db = FirebaseFirestore.getInstance()

        etUsuario = findViewById(R.id.etUsuario)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.Erregistrobtn)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val savedUser = prefs.getString("userRegistrado", null)

        // CORRECCIÓN: Si hay usuario, verificamos. NO lanzamos el intent aquí directamente.
        if (savedUser != null) {
            showLoading(true)
            verificarYEntrar(savedUser)
        }

        btnLogin.setOnClickListener { loginUser() }
        btnRegister.setOnClickListener { erregistroPantaila() }
    }

    private fun loginUser() {
        val email = etUsuario.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Bete eremu guztiak", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. ACTIVAR OVERLAY al dar al botón
        showLoading(true)

        val hashedInput = sha256(password)

        db.collection("Users")
            .whereEqualTo("email", email)
            .whereEqualTo("password", hashedInput)
            .get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                if (!queryDocumentSnapshots.isEmpty()) {
                    val userDoc = queryDocumentSnapshots.documents[0]
                    val userId = userDoc.id
                    verificarYEntrar(userId)
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Email edo pasahitza okerrak", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                showLoading(false)
                Toast.makeText(this, "Errorea zerbitzarira konektatzean", Toast.LENGTH_SHORT).show()
            }
    }

    private fun verificarYEntrar(userId: String) {
        val database = FirebaseDatabase.getInstance("https://musroyale-488aa-default-rtdb.europe-west1.firebasedatabase.app/")
        val estadoRef = database.getReference("estado_usuarios/$userId")

        estadoRef.get().addOnSuccessListener { snapshot ->
            val estado = snapshot.getValue(String::class.java)

            if (estado == "online") {
                showLoading(false)
                Toast.makeText(this, "Saioa hasita dago beste gailu batean", Toast.LENGTH_LONG).show()

                val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                prefs.edit().remove("userRegistrado").apply()
            } else {
                val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                prefs.edit().putString("userRegistrado", userId).apply()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }.addOnFailureListener {
            // 5. DESACTIVAR si falla el Realtime Database
            showLoading(false)
            Toast.makeText(this, "Konexio errorea", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }
    fun erregistroPantaila(){
        val intent = Intent(this, RegistroActivity::class.java)
        startActivity(intent)
    }

    companion object {
        // ============================
        // SHA-256 HASH
        // ============================
        fun sha256(base: String): String {
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(base.toByteArray(charset("UTF-8")))
                val hexString = StringBuilder()
                for (b in hash) {
                    val hex = Integer.toHexString(0xff and b.toInt())
                    if (hex.length == 1) hexString.append('0')
                    hexString.append(hex)
                }
                return hexString.toString()
            } catch (ex: Exception) {
                throw RuntimeException(ex)
            }
        }
    }
}