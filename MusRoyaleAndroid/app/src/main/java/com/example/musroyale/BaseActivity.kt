package com.example.musroyale

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

abstract class BaseActivity : AppCompatActivity() {

    lateinit var db: FirebaseFirestore
    lateinit var databaseRT: FirebaseDatabase
    var currentUserId: String? = null

    private var invitacionListener: ListenerRegistration? = null
    private val amigosListeners = mutableMapOf<String, ValueEventListener>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = FirebaseFirestore.getInstance()
        databaseRT = FirebaseDatabase.getInstance("https://musroyale-488aa-default-rtdb.europe-west1.firebasedatabase.app/")

        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        currentUserId = prefs.getString("userRegistrado", null)

        if (currentUserId != null) {
            iniciarEscuchasGlobales()
        }
    }

    private fun iniciarEscuchasGlobales() {
        escucharInvitaciones()
        iniciarObservadorAmigos()
    }

    // --- LÓGICA DE INVITACIONES ---
    private fun escucharInvitaciones() {
        invitacionListener = db.collection("PartidaDuo")
            .whereEqualTo("idreceptor", currentUserId)
            .whereEqualTo("onartua", false)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                snapshots?.documentChanges?.forEach { dc ->
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val doc = dc.document
                        mostrarDialogoInvitacion(doc.getString("idemisor") ?: "", doc.id)
                    }
                }
            }
    }

    private var isDialogShowing = false

    private fun mostrarDialogoInvitacion(emisorId: String, idPartida: String) {
        if (isDialogShowing) return
        isDialogShowing = true

        db.collection("Users").document(emisorId).get().addOnSuccessListener { doc ->
            if (!doc.exists()) { isDialogShowing = false; return@addOnSuccessListener }

            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_invitacion, null)
            val dialog = Dialog(this)
            dialog.setContentView(dialogView)
            dialog.setCancelable(false)

            val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressTimeout)

            // --- LÓGICA DEL TEMPORIZADOR CON BARRA ---
            val totalTime = 10000L // 10 segundos
            val timer = object : android.os.CountDownTimer(totalTime, 20) { // Actualiza cada 20ms
                override fun onTick(millisUntilFinished: Long) {
                    // Actualizamos el progreso (de 1000 a 0)
                    val progress = (millisUntilFinished * 1000 / totalTime).toInt()
                    progressBar.progress = progress
                }

                override fun onFinish() {
                    if (dialog.isShowing) {
                        db.collection("PartidaDuo").document(idPartida).delete()
                        dialog.dismiss()
                        isDialogShowing = false
                    }
                }
            }.start()
            // ------------------------------------------

            // Configuración de UI del Diálogo (Avatar, Nombre, etc.)
            dialogView.findViewById<TextView>(R.id.tvSenderName).text = doc.getString("username")
            val avatar = doc.getString("avatarActual") ?: "avadef"
            dialogView.findViewById<ImageView>(R.id.ivSenderAvatar).setImageResource(getResIdFromName(this, avatar))

            dialog.window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setGravity(Gravity.TOP)
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                attributes.y = 100
            }

            dialogView.findViewById<View>(R.id.btnAccept).setOnClickListener {
                timer.cancel() // ¡IMPORTANTE! Detener el tiempo
                val actualizaciones = mapOf(
                    "onartua" to true,
                    "jokalariak" to listOf(emisorId, currentUserId),
                    "kodea" to (1000..9999).random().toString()
                )
                db.collection("PartidaDuo").document(idPartida).update(actualizaciones).addOnSuccessListener {
                    dialog.dismiss()
                    isDialogShowing = false
                    startActivity(Intent(this, DuoActivity::class.java).apply {
                        putExtra("idPartida", idPartida)
                        putExtra("esReceptor", true)
                    })
                }
            }

            dialogView.findViewById<View>(R.id.btnDeclineInvite).setOnClickListener {
                timer.cancel()
                db.collection("PartidaDuo").document(idPartida).delete()
                dialog.dismiss()
                isDialogShowing = false
            }

            dialog.show()
        }
    }

    // --- LÓGICA DE AMIGOS ONLINE ---
    private fun iniciarObservadorAmigos() {
        db.collection("Users").document(currentUserId!!).get().addOnSuccessListener { snapshot ->
            val listaAmigos = snapshot.get("amigos") as? List<String> ?: emptyList()
            listaAmigos.forEach { id -> escucharEstadoAmigo(id) }
        }
    }

    private fun escucharEstadoAmigo(amigoId: String) {
        val ref = databaseRT.getReference("estado_usuarios/$amigoId")
        val listener = object : ValueEventListener {
            private var primeraVez = true
            override fun onDataChange(snapshot: DataSnapshot) {
                val estado = snapshot.getValue(String::class.java) ?: "offline"
                if (!primeraVez && estado == "online") {
                    obtenerDatosYMostrarBanner(amigoId)
                }
                primeraVez = false
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        amigosListeners[amigoId] = listener
    }

    private fun obtenerDatosYMostrarBanner(amigoId: String) {
        db.collection("Users").document(amigoId).get().addOnSuccessListener { doc ->
            mostrarBannerTop(doc.getString("username") ?: "Laguna", doc.getString("avatarActual"))
        }
    }

    private fun mostrarBannerTop(nombre: String, avatarStr: String?) {
        val rootLayout = findViewById<ViewGroup>(android.R.id.content)
        val layout = LayoutInflater.from(this).inflate(R.layout.layout_notification_online, rootLayout, false)

        layout.findViewById<TextView>(R.id.txtNotifyMessage).text = "$nombre konektatua!"
        val resId = getResIdFromName(this, avatarStr)
        layout.findViewById<ImageView>(R.id.imgNotifyAvatar).setImageResource(if (resId != 0) resId else R.drawable.ic_avatar3)

        rootLayout.addView(layout)
        layout.translationZ = 100f
        layout.translationY = -300f
        layout.animate().translationY(100f).setDuration(600).withEndAction {
            layout.postDelayed({
                layout.animate().translationY(-400f).alpha(0f).setDuration(500).withEndAction {
                    rootLayout.removeView(layout)
                }.start()
            }, 3500)
        }.start()
    }

    // --- UTILIDADES ---
    fun getResIdFromName(context: Context, name: String?): Int {
        if (name == null) return 0
        val cleanName = name.replace(".png", "")
        return context.resources.getIdentifier(cleanName, "drawable", context.packageName)
    }

    override fun onDestroy() {
        super.onDestroy()
        invitacionListener?.remove()
        amigosListeners.forEach { (id, listener) ->
            databaseRT.getReference("estado_usuarios/$id").removeEventListener(listener)
        }
    }
}