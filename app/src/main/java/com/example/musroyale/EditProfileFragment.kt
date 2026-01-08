package com.example.musroyale

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileFragment : Fragment() {

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        ivAvatar = root.findViewById(R.id.ivAvatar)
        btnChangeAvatar = root.findViewById(R.id.btnChangeAvatar)
        etUsername = root.findViewById(R.id.etEditUsername)
        etEmail = root.findViewById(R.id.etEditEmail)
        etPassword = root.findViewById(R.id.etEditPassword)
        etConfirm = root.findViewById(R.id.etEditConfirm)
        tvGamesPlayed = root.findViewById(R.id.tvGamesPlayed)
        tvGamesWon = root.findViewById(R.id.tvGamesWon)
        tvMoney = root.findViewById(R.id.tvMoney)
        btnSave = root.findViewById(R.id.btnSaveProfile)

        val prefs = requireActivity().getSharedPreferences("UserPrefs", 0)
        userId = prefs.getString("userRegistrado", null)

        btnChangeAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSave.setOnClickListener {
            saveProfile()
        }

        loadProfile()

        return root
    }

    private fun loadProfile() {
        userId?.let { id ->
            db.collection("Users").document(id).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        etUsername.setText(doc.getString("username") ?: "")
                        etEmail.setText(doc.getString("email") ?: "")
                        tvGamesPlayed.text = (doc.getLong("gamesPlayed") ?: 0L).toString()
                        tvGamesWon.text = (doc.getLong("gamesWon") ?: 0L).toString()
                        tvMoney.text = (doc.getString("money") ?: doc.getDouble("money")?.toString() ?: "0")
                    }
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

        val updates = HashMap<String, Any>()
        updates["username"] = username
        updates["email"] = email
        if (password.isNotEmpty()) updates["password"] = sha256(password)

        userId?.let { id ->
            db.collection("Users").document(id)
                .update(updates as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error actualizando: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    companion object {
        fun sha256(base: String): String {
            try {
                val digest = java.security.MessageDigest.getInstance("SHA-256")
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
