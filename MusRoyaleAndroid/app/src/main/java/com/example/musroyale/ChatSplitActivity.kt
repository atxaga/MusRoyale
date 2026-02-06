package com.example.musroyale

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musroyale.databinding.ActivityChatSplitBinding
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.storage.FirebaseStorage
import java.io.File
class ChatSplitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatSplitBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var friendsAdapter: FriendsChatAdapter
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private var currentUserId: String? = null

    private val messagesList = mutableListOf<ChatMessage>()
    private var selectedFriendId: String? = null
    private var chatListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatSplitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        currentUserId = prefs.getString("userRegistrado", null)
        binding.chatTitleName.text = "Mezuak kargatzen..."
        setupFriendsList()
        setupChatArea()
        cargarAmigosDelDrawer()

        binding.btnMenuFriends.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
        binding.btnCloseDrawer.setOnClickListener { binding.drawerLayout.closeDrawer(GravityCompat.START) }
        binding.btnExit.setOnClickListener { finish() }
        binding.btnCamara.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                abrirCamara()
            } else {
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) // Lo pedimos
            }
        }

        binding.btnSend.setOnClickListener { sendMessage() }
    }
    private lateinit var photoUri: Uri

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            subirImagenAFirebase(photoUri)
        }
    }
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            abrirCamara() // Si acepta, abrimos la cámara
        } else {
            Toast.makeText(this, "Kamara erabiltzeko baimenak onartu behar dituzu", Toast.LENGTH_SHORT).show()
        }
    }
    private fun abrirCamara() {
        val photoFile = File.createTempFile(
            "IMG_${System.currentTimeMillis()}_",
            ".jpg",
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )
        photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
        takePictureLauncher.launch(photoUri)
    }

    private fun subirImagenAFirebase(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val targetWidth = if (ratio > 1) 800 else (800 * ratio).toInt()
            val targetHeight = if (ratio > 1) (800 / ratio).toInt() else 800
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)

            val byteArray = outputStream.toByteArray()
            val base64Image = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)

            if (base64Image.length > 950000) {
                outputStream.reset()
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, outputStream)
                val smallByteArray = outputStream.toByteArray()
                enviarMensajeImagen(android.util.Base64.encodeToString(smallByteArray, android.util.Base64.NO_WRAP))
            } else {
                enviarMensajeImagen(base64Image)
            }

        } catch (e: Exception) {
            android.util.Log.e("Base64Error", "Error: ${e.message}")
        }
    }

    private fun enviarMensajeImagen(base64Data: String) {
        val fId = selectedFriendId ?: return
        val messageData: HashMap<String, Any> = hashMapOf(
            "idemisor" to currentUserId!!,
            "idreceptor" to fId,
            "mensaje" to "📷 Imagen",
            "imageUrl" to base64Data, // Aquí va el String gigante
            "timestamp" to System.currentTimeMillis(),
            "leido" to false
        )

        db.collection("Chats").add(messageData)
            .addOnSuccessListener {
                Toast.makeText(this, "Irudia bidalita!", Toast.LENGTH_SHORT).show()
            }
    }


    private fun setupFriendsList() {
        friendsAdapter = FriendsChatAdapter(mutableListOf()) { friend ->
            selectedFriendId = friend.id

            val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            prefs.edit().putString("lastChatFriendId", friend.id).apply()

            escucharChatReal(friend)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        binding.recyclerFriends.layoutManager = LinearLayoutManager(this)
        binding.recyclerFriends.adapter = friendsAdapter
    }

    private var friendStatusListener: com.google.firebase.database.ValueEventListener? = null
    private var friendStatusRef: com.google.firebase.database.DatabaseReference? = null
    private fun escucharChatReal(friend: Friend) {
        binding.chatTitleName.text = friend.name
        chatListener?.remove()
        friendStatusListener?.let { friendStatusRef?.removeEventListener(it) } // Limpiar anterior

        friendStatusRef = com.google.firebase.database.FirebaseDatabase
            .getInstance("https://musroyale-488aa-default-rtdb.europe-west1.firebasedatabase.app/")
            .getReference("estado_usuarios")
            .child(friend.id)

        friendStatusListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val estado = snapshot.getValue(String::class.java) ?: "offline"
                binding.chatTitleStatus.visibility = if (estado == "online") View.VISIBLE else View.GONE
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        friendStatusRef?.addValueEventListener(friendStatusListener!!)
        chatListener = db.collection("Chats")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                val batch = db.batch()
                var hayCambiosPorMarcar = false

                messagesList.clear()
                snapshot?.documents?.forEach { doc ->
                    val emisor = doc.getString("idemisor") ?: ""
                    val receptor = doc.getString("idreceptor") ?: ""
                    val leido = doc.getBoolean("leido") ?: false

                    if ((emisor == currentUserId && receptor == friend.id) ||
                        (emisor == friend.id && receptor == currentUserId)) {

                        messagesList.add(ChatMessage(
                            senderId = emisor,
                            receiverId = receptor,
                            message = doc.getString("mensaje") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isMine = emisor == currentUserId,
                            leido = doc.getBoolean("leido") ?: false,
                            imageUrl = doc.getString("imageUrl") // <--- ¡AÑADE ESTA LÍNEA!
                        ))

                        if (receptor == currentUserId && emisor == friend.id && !leido) {
                            batch.update(doc.reference, "leido", true)
                            hayCambiosPorMarcar = true
                        }
                    }
                }

                if (hayCambiosPorMarcar) {
                    batch.commit().addOnSuccessListener {
                    }
                }

                chatAdapter.notifyDataSetChanged()
                if (messagesList.isNotEmpty()) {
                    binding.recyclerChat.scrollToPosition(messagesList.size - 1)
                }
            }
    }

    private fun sendMessage() {
        val text = binding.editMessage.text.toString().trim()
        val fId = selectedFriendId

        if (text.isNotEmpty() && currentUserId != null && fId != null) {
            var messageData: HashMap<String, Any> = hashMapOf(
                "idemisor" to currentUserId!!,
                "idreceptor" to fId,
                "mensaje" to text,
                "timestamp" to System.currentTimeMillis(),
                "leido" to false
            )

            db.collection("Chats")
                .add(messageData)
                .addOnSuccessListener {
                    binding.editMessage.setText("")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("FirestoreError", "Error al enviar", e)
                }
        }
    }

    private fun setupChatArea() {
        chatAdapter = ChatAdapter(messagesList)
        binding.recyclerChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerChat.adapter = chatAdapter
    }

    private var friendsListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun cargarAmigosDelDrawer() {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentUserEmail = prefs.getString("userRegistrado", null) ?: return

        friendsListener = db.collection("Users").document(currentUserEmail)
            .addSnapshotListener { document, _ ->
                val amigosIds = document?.get("amigos") as? List<String> ?: listOf()

                if (amigosIds.isNotEmpty()) {
                    db.collection("Users")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), amigosIds)
                        .get()
                        .addOnSuccessListener { query ->
                            val listaAmigos = query.documents.map {
                                Friend(it.id, it.getString("username") ?: "Usuario",0, it.getString("avatarActual") ?: "avadef.png", )
                            }

                            // --- INICIO LÓGICA DE CONTEO ---
                            for (friend in listaAmigos) {
                                db.collection("Chats")
                                    .whereEqualTo("idemisor", friend.id)
                                    .whereEqualTo("idreceptor", currentUserId)
                                    .whereEqualTo("leido", false)
                                    .addSnapshotListener { snapshots, _ ->
                                        val count = snapshots?.size() ?: 0
                                        friend.unreadCount = count
                                        friendsAdapter.notifyDataSetChanged()
                                    }
                            }
                            // --- FIN LÓGICA DE CONTEO ---

                            friendsAdapter.updateData(listaAmigos)

                            if (listaAmigos.isNotEmpty() && selectedFriendId == null) {
                                val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                val lastId = prefs.getString("lastChatFriendId", null)

                                // 1. Intentamos buscar al último con el que se habló
                                val friendToOpen = listaAmigos.find { it.id == lastId }
                                // 2. Si no existe o es nulo, cogemos al primero de la lista
                                    ?: listaAmigos.firstOrNull()

                                friendToOpen?.let { firstFriend ->
                                    selectedFriendId = firstFriend.id
                                    // Esto activa el chat automáticamente
                                    escucharChatReal(firstFriend)

                                    // Opcional: Si quieres que el drawer sepa que está seleccionado
                                    // podrías añadir una variable en tu adapter para resaltar el item
                                }
                            }
                        }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatListener?.remove() // Importante limpiar al salir de la pantalla
    }
}