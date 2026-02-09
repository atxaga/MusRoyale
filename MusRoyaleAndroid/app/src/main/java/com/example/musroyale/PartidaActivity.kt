package com.example.musroyale

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import android.widget.TextView
import kotlin.unaryMinus

class PartidaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PARAM = "com.example.musroyale.EXTRA_PARAM"
        const val EXTRA_CODE = "com.example.musroyale.EXTRA_CODE"
    }

    private val serverHost = "3.234.215.132"
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    private val serverPort = 13000
    private val connectTimeoutMs = 20000
    private val currentCards = mutableListOf<String>()
    private val selectedIndices = mutableSetOf<Int>()

    private lateinit var bottomCard1: ImageView
    private lateinit var bottomCard2: ImageView
    private lateinit var bottomCard3: ImageView
    private lateinit var bottomCard4: ImageView
    private lateinit var roundLabel: TextView
    private var puntosApostar = 2
    private lateinit var layoutSelector: android.widget.LinearLayout // Usamos el nombre completo si hay dudas con los imports
    private var ordagoOn: Boolean = false
    private var envidoOn: Boolean = false

    private val listaEsperaJugadores = mutableListOf<Triple<Int, String, Int>>()
    private var miTalde: Int = -1
    private var decisionContinuation: kotlinx.coroutines.CancellableContinuation<String>? = null
    private lateinit var goian: FrameLayout
    private lateinit var behean: FrameLayout
    private lateinit var eskuin: FrameLayout
    private lateinit var ezker: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Establecer el layout directamente
        setContentView(R.layout.activity_partida)

        val iconoAjustes = findViewById<ImageView>(R.id.salir)
        val txtUtzi = findViewById<TextView>(R.id.txtPartidaUtzi)
        roundLabel = findViewById(R.id.roundLabel)

        iconoAjustes.setOnClickListener {
            if (txtUtzi.visibility == View.GONE) {
                txtUtzi.visibility = View.VISIBLE
                txtUtzi.postDelayed({
                    txtUtzi.visibility = View.GONE
                }, 3000)
            } else {
                txtUtzi.visibility = View.GONE
            }
        }
        // Dentro de onCreate()
        layoutSelector = findViewById(R.id.layoutSelectorEnvido)
        val txtCantidad = findViewById<TextView>(R.id.txtCantidadEnvido)

        // --- LÓGICA DEL SELECTOR BEIGE (ENVITE) ---

        // 1. Mostrar el selector al pulsar ENVIDO
        findViewById<Button>(R.id.btnEnvido).setOnClickListener {
            // Escondemos botones exteriores
            it.visibility = View.GONE
            findViewById<Button>(R.id.btnOrdago).visibility = View.GONE
            findViewById<Button>(R.id.btnPasar).visibility = View.GONE

            // Mostramos panel beige con animación
            layoutSelector.visibility = View.VISIBLE
            layoutSelector.alpha = 0f
            layoutSelector.animate().alpha(1f).setDuration(200)

            puntosApostar = 2
            txtCantidad.text = "2"
        }

        // 2. Botones de + y -
        findViewById<Button>(R.id.btnPlus).setOnClickListener {
            if (puntosApostar < 40) {
                puntosApostar++
                txtCantidad.text = puntosApostar.toString()
            }
        }

        findViewById<Button>(R.id.btnMinus).setOnClickListener {
            if (puntosApostar > 2) {
                puntosApostar--
                txtCantidad.text = puntosApostar.toString()
            }
        }

        findViewById<Button>(R.id.btnConfirmarEnvido).setOnClickListener {
            decisionContinuation?.resume(puntosApostar.toString(), null)
            layoutSelector.visibility = View.GONE
        }

        findViewById<Button>(R.id.btnOrdago).setOnClickListener {
            decisionContinuation?.resume("ordago", null)
            layoutSelector.visibility = View.GONE
        }
        txtUtzi.setOnClickListener {
            val view = layoutInflater.inflate(R.layout.dialog_custom_exit, null)
            val dialog = android.app.Dialog(this)
            dialog.setContentView(view)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val btnSi = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSi)
            val btnNo = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNo)

            btnSi.setOnClickListener {
                dialog.dismiss()
                finish()
            }

            btnNo.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        // Inicializar vistas de cartas
        bottomCard1 = findViewById(R.id.bottomCard1)
        bottomCard2 = findViewById(R.id.bottomCard2)
        bottomCard3 = findViewById(R.id.bottomCard3)
        bottomCard4 = findViewById(R.id.bottomCard4)
        goian= findViewById(R.id.goian)
        behean= findViewById(R.id.cardsBottomFrame)
        eskuin= findViewById(R.id.eskuina)
        ezker= findViewById(R.id.ezkerra)

        goian.visibility=View.GONE
        behean.visibility=View.GONE
        eskuin.visibility=View.GONE
        ezker.visibility=View.GONE

        setupCardClick(bottomCard1, 0)
        setupCardClick(bottomCard2, 1)
        setupCardClick(bottomCard3, 2)
        setupCardClick(bottomCard4, 3)

        // --- LISTENERS DE LOS BOTONES CON R.ID ---
        findViewById<Button>(R.id.btnMus).setOnClickListener {
            decisionContinuation?.resume("mus", null)
        }

        findViewById<Button>(R.id.btnPasar).setOnClickListener {
            decisionContinuation?.resume("paso", null)
        }
        findViewById<Button>(R.id.btnDeskartea).setOnClickListener {
            val discardString = buildDiscardString()
            decisionContinuation?.resume(discardString, null)
        }
        findViewById<Button>(R.id.btnEnvido).setOnClickListener {
            decisionContinuation?.resume(txtCantidad.toString(), null)
        }
        findViewById<Button>(R.id.btnQuiero).setOnClickListener {
            decisionContinuation?.resume("quiero", null)
        }


        partidaHasi()
    }

    private fun setupCardClick(view: ImageView, index: Int) {
        view.setOnClickListener {
            if (selectedIndices.contains(index)) {
                selectedIndices.remove(index)
                view.alpha = 1f
            } else {
                selectedIndices.add(index)
                view.alpha = 0.5f
            }
        }
    }

    fun partidaHasi() {
        lifecycleScope.launch(Dispatchers.IO) {
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(serverHost, serverPort), connectTimeoutMs)

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = socket.getOutputStream().bufferedWriter()


                val received = intent.getStringExtra(EXTRA_PARAM)
                Log.e("PartidaActivity", "Enviando al servidor: $received")
                writer.write(received)
                writer.newLine()
                writer.flush()

                withContext(Dispatchers.Main) {
                    roundLabel.text = "BILATZEN..."
                }
                val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                val nireid = prefs.getString("userRegistrado", "") ?: ""

                if (nireid.isNotEmpty()) {
                    writer.write(nireid)
                    writer.newLine()
                    writer.flush()
                }
                while (true) {

                    val serverMsg = reader.readLine() ?: break

                    when {
                        serverMsg.startsWith("ACTION:") -> {
                            val partes = serverMsg.split(":")
                            if (partes.size >= 3) {
                                withContext(Dispatchers.Main) {
                                    runOnUiThread { mostrarDecision(partes[1].toInt(), partes[2]) }
                                }
                            }
                        }
                        serverMsg.startsWith("INFO:") -> {
                            val datuak = serverMsg.substringAfter("INFO:")
                            listaEsperaJugadores.clear()

                            val bloques = datuak.split(",")
                            bloques.forEach { bloque ->
                                val trimmed = bloque.trim()

                                if (trimmed.length >= 3) {
                                    val equipo = trimmed.first().toString().toInt()

                                    val turno = trimmed.last().toString().toInt()

                                    val id = trimmed.substring(1, trimmed.length - 1)

                                    jokalarienInfo(equipo, id, turno)
                                }
                            }
                        }

                        serverMsg == "CARDS" -> {
                            withContext(Dispatchers.Main) {
                                roundLabel.text = "BANATZEN"
                                goian.visibility=View.VISIBLE
                                behean.visibility=View.VISIBLE
                                eskuin.visibility=View.VISIBLE
                                ezker.visibility=View.VISIBLE
                            }
                            recibirCartas(reader, 4)

                        }

                        serverMsg == "TURN" -> {
                            withContext(Dispatchers.Main) {
                                toggleDecisionButtons(visible = true)
                                Toast.makeText(this@PartidaActivity, "Zure txanda!", Toast.LENGTH_SHORT).show()
                            }

                            val respuesta = kotlinx.coroutines.suspendCancellableCoroutine<String> { cont ->
                                decisionContinuation = cont
                            }

                            writer.write(respuesta)
                            writer.newLine()
                            writer.flush()

                            withContext(Dispatchers.Main) {
                                toggleDecisionButtons(visible = false)
                            }
                        }

                        serverMsg == "ALL_MUS" -> {
                            withContext(Dispatchers.Main) {
                                findViewById<Button>(R.id.btnDeskartea).visibility = View.VISIBLE
                            }

                            val deskarteRespuesta = kotlinx.coroutines.suspendCancellableCoroutine<String> { cont ->
                                decisionContinuation = cont
                            }

                            writer.write(deskarteRespuesta)
                            writer.newLine()
                            writer.flush()

                            withContext(Dispatchers.Main) {
                                limpiarCartasDescartadas()
                                findViewById<Button>(R.id.btnDeskartea).visibility = View.GONE
                            }
                            recibirCartas(reader, 4)
                        }

                        // Agrupamos las fases de juego
                        serverMsg == "GRANDES" || serverMsg == "PEQUEÑAS" || serverMsg == "PARES" || serverMsg == "JUEGO" || serverMsg == "PUNTO" -> {
                            withContext(Dispatchers.Main) {
                                roundLabel.text = serverMsg
                                toggleEnvidoButtons(visible = true)
                                Toast.makeText(this@PartidaActivity, "$serverMsg jolasten!", Toast.LENGTH_SHORT).show()
                            }

                            val respuesta = kotlinx.coroutines.suspendCancellableCoroutine<String> { cont ->
                                decisionContinuation = cont
                            }

                            writer.write(respuesta)
                            writer.newLine()
                            writer.flush()

                            withContext(Dispatchers.Main) {
                                findViewById<Button>(R.id.btnEnvido).visibility = View.GONE
                                findViewById<Button>(R.id.btnQuiero).visibility = View.GONE
                                findViewById<Button>(R.id.btnPasar).visibility = View.GONE
                                findViewById<Button>(R.id.btnOrdago).visibility = View.GONE
                            }
                            ordagoOn = false
                            envidoOn = false
                        }

                        serverMsg == "ORDAGO" -> ordagoOn = true
                        serverMsg == "ENVIDO" -> envidoOn = true

                        serverMsg == "PUNTUAKJASO" -> {
                            val left1 = reader.readLine()
                            val left2 = reader.readLine()
                            val right1 = reader.readLine()
                            val right2 = reader.readLine()
                            withContext(Dispatchers.Main) {
                                roundLabel.text = "Puntuazioa"
                                findViewById<TextView>(R.id.leftScoreBox1).text = left1
                                findViewById<TextView>(R.id.leftScoreBox2).text = left2
                                findViewById<TextView>(R.id.rightScoreBox1).text = right1
                                findViewById<TextView>(R.id.rightScoreBox2).text = right2
                            }
                        }
                        serverMsg == "PEDIR_CODIGO" -> {
                            val kodea = intent.getStringExtra(EXTRA_CODE) ?: ""
                            writer.write(kodea)
                            writer.newLine()
                            writer.flush()
                        }
                        serverMsg == "ERABAKIA" -> {
                            val erabakia = reader.readLine() ?: ""

                            try {
                                val parts = erabakia.split(",", limit = 3)
                                if (parts.size >= 3) {
                                    val uid = parts[0].trim()
                                    val serverId = parts[1].trim().toIntOrNull() ?: 0
                                    var mensaje = parts[2].trim()
                                    if (mensaje.endsWith("PARES")){
                                        if (mensaje.startsWith("jokuaDaukat")){
                                            mensaje = "PARES DAUKAT"
                                        }else{
                                            mensaje = "PARES EZ DUT"
                                        }
                                    }else if (mensaje.endsWith("JUEGO")){
                                        if (mensaje.startsWith("jokuaDaukat")){
                                            mensaje = "JUEGO DAUKAT"
                                        }else{
                                            mensaje = "JUEGO EZ DUT"
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        showErabakiaPopup(serverId, uid, mensaje)
                                    }
                                } else {
                                    Log.e("PartidaActivity", "ERABAKIA formato inesperado: $erabakia")
                                }
                            } catch (e: Exception) {
                                Log.e("PartidaActivity", "Error procesando ERABAKIA: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PartidaActivity, "Konexio errorea: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("PartidaActivity", e.message, e)
                }
            } finally {
                socket?.close()
            }
        }
    }
    private fun getAnchorIdForServerId(serverId: Int): Int {
        return when (serverId) {
            0 -> R.id.infoBottom
            1 -> R.id.infoLeft
            2 -> R.id.infoTop
            3 -> R.id.infoRight
            else -> R.id.infoBottom
        }
    }
    private fun showErabakiaPopup(serverId: Int, uid: String, mensaje: String) {
        runOnUiThread {
            try {
                val anchorId = getAnchorIdForServerId(serverId)
                val anchorView = findViewById<View>(anchorId) ?: return@runOnUiThread

                val popupView = layoutInflater.inflate(R.layout.popup_erabakia, null)
                val tv = popupView.findViewById<TextView>(R.id.popupText)
                tv.text = "${uid}: ${mensaje}"

                val popup = PopupWindow(
                    popupView,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    false
                ).apply {
                    elevation = 12f
                    isOutsideTouchable = true
                    isTouchable = false
                }

                // Intentar mostrar encima del ancla (offset negativo)
                val yOffset = -anchorView.height - 16
                val xOffset = 0

                // showAsDropDown puede situarlo justo encima usando offset negativo
                popup.showAsDropDown(anchorView, xOffset, yOffset, Gravity.CENTER)

                // Cerrar después de 3 segundos
                Handler(Looper.getMainLooper()).postDelayed({
                    if (popup.isShowing) popup.dismiss()
                }, 3000)
            } catch (e: Exception) {
                Log.e("PartidaActivity", "Error mostrando popup ERABAKIA: ${e.message}")
            }
        }
    }
    private fun jokalarienInfo(taldea: Int, jokalariID: String, zerbitzariId: Int) {
        listaEsperaJugadores.add(Triple(taldea, jokalariID, zerbitzariId))

        if (listaEsperaJugadores.size == 4) {
            val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val miId = prefs.getString("userRegistrado", "") ?: ""

            val yo = listaEsperaJugadores.find { it.second == miId }

            if (yo != null) {
                val miTurno = yo.third

                val turnoDerecha = (miTurno + 1) % 4
                val turnoIzquierda = (miTurno + 3) % 4

                    for (jugador in listaEsperaJugadores) {
                    val suId = jugador.second
                    val suTurno = jugador.third

                    if (suId == miId) {
                        cargarInfoEnVista(suId, "Bottom") // Yo abajo
                    } else if (suTurno == turnoDerecha) {
                        cargarInfoEnVista(suId, "Right")  // El siguiente a la derecha
                    } else if (suTurno == turnoIzquierda) {
                        cargarInfoEnVista(suId, "Left")   // El anterior a la izquierda
                    } else {
                        cargarInfoEnVista(suId, "Top")    // El que queda, enfrente (pareja)
                    }
                }
            }

            listaEsperaJugadores.clear()
        }
    }

    private fun cargarInfoEnVista(uid: String, posicion: String) {
        db.collection("Users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nombre = document.getString("username") ?: "Jokalaria"

                    var avatarName = document.getString("avatarActual") ?: "avadef"
                    avatarName = avatarName.replace(".png", "").replace(".jpg", "")

                    runOnUiThread {
                        try {
                            val txtId = resources.getIdentifier("name$posicion", "id", packageName)
                            if (txtId != 0) {
                                findViewById<TextView>(txtId).text = nombre.uppercase()
                            }

                            // Actualizar Avatar
                            val imgId = resources.getIdentifier("avatar$posicion", "id", packageName)
                            if (imgId != 0) {
                                val avatarImageView = findViewById<ImageView>(imgId)

                                // Buscamos el dibujo sin el .png
                                val resDrawableId = resources.getIdentifier(avatarName, "drawable", packageName)

                                if (resDrawableId != 0) {
                                    avatarImageView.setImageResource(resDrawableId)
                                } else {
                                    // Si no lo encuentra, ponemos uno por defecto (asegúrate de que este nombre existe)
                                    avatarImageView.setImageResource(R.drawable.avarat_circle_bg)
                                    Log.e("PartidaActivity", "No se encontró el drawable: $avatarName")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("PartidaActivity", "Error en vista $posicion: ${e.message}")
                        }
                    }
                }
            }
    }

    private fun mostrarDecision(playerZnb: Int, mensaje: String) {
        val statusId = when (playerZnb) {
            0 -> R.id.statusBottom
            1 -> R.id.statusLeft
            2 -> R.id.statusTop
            3 -> R.id.statusRight
            else -> null
        }

        statusId?.let { id ->
            val tv = findViewById<TextView>(id)
            tv.text = mensaje.uppercase()
            tv.visibility = View.VISIBLE

            when (mensaje.lowercase()) {
                "mus" -> tv.setTextColor(android.graphics.Color.parseColor("#FFEB3B")) // Amarillo
                "envido", "2" -> tv.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Verde
                "ordago" -> tv.setTextColor(android.graphics.Color.parseColor("#FF5252")) // Rojo
                else -> tv.setTextColor(android.graphics.Color.WHITE)
            }

            tv.alpha = 0f
            tv.scaleX = 0.5f
            tv.scaleY = 0.5f
            tv.animate()
                .alpha(1f)
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(300)
                .withEndAction {
                    tv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100)
                }

            // 4. Desvanecer después de 3 segundos
            tv.removeCallbacks(null) // Limpiar callbacks previos si el jugador habla rápido
            tv.postDelayed({
                tv.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(400)
                    .withEndAction {
                        tv.visibility = View.GONE
                    }
            }, 3000)
        }
    }

    private suspend fun recibirCartas(reader: BufferedReader, cantidad: Int) {
        currentCards.clear()
        val views = listOf(bottomCard1, bottomCard2, bottomCard3, bottomCard4)

        repeat(cantidad) { i ->
            val karta = reader.readLine() ?: return@repeat
            currentCards.add(karta)

            withContext(Dispatchers.Main) {
                val resId = resources.getIdentifier(karta, "drawable", packageName)
                views[i].setImageResource(resId)
                views[i].visibility = View.VISIBLE
                views[i].alpha = 1f
            }
        }

        withContext(Dispatchers.Main) {
            selectedIndices.clear()
        }
    }

    private fun limpiarCartasDescartadas() {
        val views = listOf(bottomCard1, bottomCard2, bottomCard3, bottomCard4)
        for (index in selectedIndices) {
            views[index].setImageResource(0)
            views[index].alpha = 1f
        }
    }

    private fun toggleDecisionButtons(visible: Boolean) {
        val estado = if (visible) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnMus).visibility = estado
        findViewById<Button>(R.id.btnPasar).visibility = estado
    }

    private fun toggleEnvidoButtons(visible: Boolean) {
        val estado = if (visible) View.VISIBLE else View.GONE

        if (!visible) {
            layoutSelector.visibility = View.GONE
            findViewById<Button>(R.id.btnEnvido).visibility = View.GONE
            findViewById<Button>(R.id.btnOrdago).visibility = View.GONE
            findViewById<Button>(R.id.btnPasar).visibility = View.GONE
            findViewById<Button>(R.id.btnQuiero).visibility = View.GONE
        } else {
            // Al empezar turno, mostramos botones base (Envido y Órdago visibles si no hay Órdago previo)
            findViewById<Button>(R.id.btnEnvido).visibility = if (ordagoOn) View.GONE else View.VISIBLE
            findViewById<Button>(R.id.btnOrdago).visibility = if (ordagoOn) View.GONE else View.VISIBLE
            findViewById<Button>(R.id.btnPasar).visibility = View.VISIBLE
            findViewById<Button>(R.id.btnQuiero).visibility = if (envidoOn || ordagoOn) View.VISIBLE else View.GONE
        }
    }

    private fun buildDiscardString(): String {
        if (selectedIndices.isEmpty()) return "*"
        val seleccionadas = selectedIndices.map { currentCards[it] }
        return seleccionadas.joinToString("-") + "*"
    }
}