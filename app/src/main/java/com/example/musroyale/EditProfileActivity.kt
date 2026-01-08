package com.example.musroyale

import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.Exception
import java.security.MessageDigest

class EditProfileActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var btnChangeAvatar: Button
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirm: EditText
    private lateinit var tvGamesPlayed: TextView
    private lateinit var tvGamesWon: TextView
    private lateinit var tvMoney: TextView
    private lateinit var btnSave: Button

    private val db = FirebaseFirestore.getInstance()

    private var userId: String? = null
    private var avatarUriToUpload: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            avatarUriToUpload = uri
            ivAvatar.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        ivAvatar = findViewById(R.id.ivAvatar)
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar)
        etUsername = findViewById(R.id.etEditUsername)
        etEmail = findViewById(R.id.etEditEmail)
        etPassword = findViewById(R.id.etEditPassword)
        etConfirm = findViewById(R.id.etEditConfirm)
        tvGamesPlayed = findViewById(R.id.tvGamesPlayed)
        tvGamesWon = findViewById(R.id.tvGamesWon)
        tvMoney = findViewById(R.id.tvMoney)
        btnSave = findViewById(R.id.btnSaveProfile)

        userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("userRegistrado", null)
        if (userId == null) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnChangeAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSave.setOnClickListener {
            saveProfile()
        }

        loadProfile()
    }

    private fun loadProfile() {
        userId?.let { id ->
            db.collection("Users").document(id).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        etUsername.setText(doc.getString("username") ?: "")
                        etEmail.setText(doc.getString("email") ?: "")
                        val avatarUrl = doc.getString("avatarUrl")
                        // Cargar avatar si existe (si usas Glide/Picasso, reemplaza)
                        avatarUrl?.let {
                            // Glide.with(this).load(it).into(ivAvatar)
                        }

                        tvGamesPlayed.text = (doc.getLong("gamesPlayed") ?: 0L).toString()
                        tvGamesWon.text = (doc.getLong("gamesWon") ?: 0L).toString()
                        tvMoney.text = (doc.getString("money") ?: doc.getDouble("money")?.toString() ?: "0")
                    }
                }
                .addOnFailureListener { e: Exception ->
                    Toast.makeText(this, "Error cargando perfil: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun saveProfile() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirm = etConfirm.text.toString()

        if (username.isEmpty()) {
            etUsername.error = "Introduce un nombre"
            etUsername.requestFocus()
            return
        }
        if (email.isEmpty()) {
            etEmail.error = "Introduce un email"
            etEmail.requestFocus()
            return
        }
        if (password.isNotEmpty()) {
            if (password.length < 6) {
                etPassword.error = "La contraseña debe tener al menos 6 caracteres"
                etPassword.requestFocus()
                return
            }
            if (password != confirm) {
                etConfirm.error = "Las contraseñas no coinciden"
                etConfirm.requestFocus()
                return
            }
        }

        // Para pruebas temporales, no subimos avatar a Storage. Solo actualizamos campos en Firestore.
        val updates = HashMap<String, Any>()
        updates["username"] = username
        updates["email"] = email
        if (password.isNotEmpty()) {
            updates["password"] = sha256(password)
        }

        userId?.let { id ->
            db.collection("Users").document(id)
                .update(updates as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    etPassword.text.clear()
                    etConfirm.text.clear()
                    loadProfile()
                }
                .addOnFailureListener { e: Exception ->
                    Toast.makeText(this, "Error actualizando: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    companion object {
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