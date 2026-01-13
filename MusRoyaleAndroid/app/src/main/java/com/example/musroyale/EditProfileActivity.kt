package com.example.musroyale

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.musroyale.databinding.ActivityEditProfileBinding
import com.example.musroyale.databinding.FragmentEditProfileBinding // Asegúrate de que este nombre coincida con tu XML
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest

class EditProfileActivity : AppCompatActivity() {

    // Se genera automáticamente según el nombre de tu layout: fragment_edit_profile.xml
    private lateinit var binding: ActivityEditProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Binding
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener ID del usuario desde SharedPreferences
        val prefs = getSharedPreferences("UserPrefs", 0)
        userId = prefs.getString("userRegistrado", null)

        loadProfile()

        // Configurar Listeners usando binding.ID
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }


    }

    private fun loadProfile() {
        userId?.let { id ->
            db.collection("Users").document(id).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        // Acceso directo a las vistas mediante binding
                        binding.etEditUsername.setText(doc.getString("username") ?: "")
                        binding.etEditEmail.setText(doc.getString("email") ?: "")

                        val saldo = doc.getString("dinero") ?: "0.00"
                        binding.tvMoney.text = "$saldo USDT"

                        // Carga de avatar
                        val nombreImagen = doc.getString("avatarActual")
                        if (!nombreImagen.isNullOrEmpty()) {
                            val resId = resources.getIdentifier(
                                nombreImagen.replace(".png", ""),
                                "drawable",
                                packageName
                            )
                            if (resId != 0) {
                                binding.ivAvatar.setImageResource(resId)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveProfile() {
        val nuevoUser = binding.etEditUsername.text.toString().trim()
        val nuevoEmail = binding.etEditEmail.text.toString().trim()
        val pass = binding.etEditPassword.text.toString()

        if (nuevoUser.isEmpty()) {
            binding.etEditUsername.error = "El nombre no puede estar vacío"
            return
        }

        val updates = mutableMapOf<String, Any>(
            "username" to nuevoUser,
            "email" to nuevoEmail
        )

        if (pass.isNotEmpty()) {
            if (pass.length < 6) {
                binding.etEditPassword.error = "Mínimo 6 caracteres"
                return
            }
            updates["password"] = sha256(pass)
        }

        userId?.let { id ->
            db.collection("Users").document(id).update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "¡Perfil actualizado!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun sha256(base: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(base.toByteArray(charset("UTF-8")))
        return hash.joinToString("") { "%02x".format(it) }
    }
}