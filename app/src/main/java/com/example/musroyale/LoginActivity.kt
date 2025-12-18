package com.example.musroyale

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Inicializar Firestore
        db = FirebaseFirestore.getInstance()

        etUsuario = findViewById(R.id.etUsuario)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.Erregistrobtn)

        btnLogin.setOnClickListener(View.OnClickListener { v: View? -> loginUser() })
        btnRegister.setOnClickListener(View.OnClickListener { v: View? -> erregistroPantaila() })
    }

    // ============================
    // LOGIN
    // ============================
    private fun loginUser() {
        val email = etUsuario.getText().toString().trim { it <= ' ' }
        val password = etPassword.getText().toString().trim { it <= ' ' }

        if (email.isEmpty()) {
            etUsuario.setError("Erabiltzailearen izena satu behar duzu")
            etUsuario.requestFocus()
            return
        }
        if (password.isEmpty()) {
            etPassword.setError("Pasahitza sartu behar duzu")
            etPassword.requestFocus()
            return
        }

        val hashedInput = sha256(password)

        db.collection("Users")
            .whereEqualTo("email", email)
            .whereEqualTo("password", hashedInput)
            .get()
            .addOnSuccessListener({ queryDocumentSnapshots ->
                if (!queryDocumentSnapshots.isEmpty()) {
                    // User Aurkituta
                    val userDoc: DocumentSnapshot = queryDocumentSnapshots.getDocuments().get(0)
                    Toast.makeText(
                        this,
                        "Bienvenido " + userDoc.getString("email"),
                        Toast.LENGTH_SHORT

                    ).show()
                    startActivity(Intent(this, MainActivity::class.java))

                } else {
                    Toast.makeText(this, "Email edo pasahitza okerrak", Toast.LENGTH_LONG).show()
                }
            })
            .addOnFailureListener({ e ->
                Toast.makeText(
                    this,
                    "Error: " + e.toString(),
                    Toast.LENGTH_LONG
                ).show()
            })
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