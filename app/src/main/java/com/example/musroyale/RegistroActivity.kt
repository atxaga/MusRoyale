package com.example.musroyale

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest

class RegistroActivity : AppCompatActivity() {

    private lateinit var etUsuario: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirm: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvHaveAccount: TextView

    // Firestore instance
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etUsuario = findViewById(R.id.etRegUsuario)
        etEmail = findViewById(R.id.etRegEmail)
        etPassword = findViewById(R.id.etRegPassword)
        etConfirm = findViewById(R.id.errepikatuPass)
        btnRegister = findViewById(R.id.btnRegister)
        tvHaveAccount = findViewById(R.id.Loginbtn)

        db = FirebaseFirestore.getInstance()

        btnRegister.setOnClickListener {
            registerUser()
        }

        tvHaveAccount.setOnClickListener {
            // Volver al login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    // ============================
    // REGISTRO
    // ============================
    private fun registerUser() {
        val username = etUsuario.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirm = etConfirm.text.toString()

        // Balidazioak
        if (username.isEmpty()) {
            etUsuario.error = "Erabiltzaile izen bat sartu"
            etUsuario.requestFocus()
            return
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Email zuzena sartu"
            etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "Pasahitza bat sartu"
            etPassword.requestFocus()
            return
        }
        if (password.length < 6) {
            etPassword.error = "Pasahitza 6 karaktere izan behar ditu gutxienez"
            etPassword.requestFocus()
            return
        }
        if (password != confirm) {
            etConfirm.error = "Pasahitzak ez dira berdinak"
            etConfirm.requestFocus()
            return
        }

        // Berifikatu aurretik existitzen den, lehenik email-a
        db.collection("Users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { emailQuery ->
                if (!emailQuery.isEmpty) {
                    Toast.makeText(this, "Email hau erregistratuta dago", Toast.LENGTH_LONG).show()
                } else {
                    // Email-a existitzen bada, izena komprobatzen dugu
                    db.collection("Users")
                        .whereEqualTo("username", username)
                        .get()
                        .addOnSuccessListener { userQuery ->
                            if (!userQuery.isEmpty) {
                                Toast.makeText(this, "User-aren izena erregistratuta dago", Toast.LENGTH_LONG).show()
                            } else {
                                // User berria sortu
                                val hashedPassword = sha256(password)

                                val userMap: MutableMap<String, Any> = HashMap()
                                userMap["email"] = email
                                userMap["username"] = username
                                userMap["password"] = hashedPassword
                                userMap["dinero"] = "0"
                                userMap["createdAt"] = FieldValue.serverTimestamp()

                                db.collection("Users")
                                    .add(userMap)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            this,
                                            "Erregistratuta",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        val userId = it.id
                                        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                        prefs.edit().putString("userRegistrado", userId).apply()


                                        val intent = Intent(this, MainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(
                                            this,
                                            "Erabiltzailea erregistratzen Errorea: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Erabiltzailearen izena konprobatzen Errorea: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Email-a konprobatzen Errorea: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
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