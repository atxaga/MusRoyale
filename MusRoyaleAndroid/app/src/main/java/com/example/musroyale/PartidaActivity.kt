package com.example.musroyale

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.unaryMinus

class PartidaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PARAM = "com.example.musroyale.EXTRA_PARAM"
        const val EXTRA_CODE = "com.example.musroyale.EXTRA_CODE"
    }

    private val serverHost = "52.72.136.36"
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    private val serverPort = 13000
    private val connectTimeoutMs = 20000
    private val currentCards = mutableListOf<String>()
    private val selectedIndices = mutableSetOf<Int>()
    // Dentro de la clase, antes del onCreate
    private lateinit var btnMus: com.google.android.material.button.MaterialButton
    private lateinit var btnPasar: com.google.android.material.button.MaterialButton

    private lateinit var btnEnvidoMas: com.google.android.material.button.MaterialButton
    private lateinit var btnQuiero: com.google.android.material.button.MaterialButton
    private lateinit var btnDeskartea: com.google.android.material.button.MaterialButton
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
    private var puntosEquipo1 = 0
    private var puntosEquipo2 = 0
    private var turnoID = ""
    private var apuestaRealizada = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Establecer el layout directamente
        setContentView(R.layout.activity_partida)

        btnMus = findViewById(R.id.btnMus)
        btnPasar = findViewById(R.id.btnPasar)

        btnEnvidoMas = findViewById(R.id.btnEnvidoMas)
        btnQuiero = findViewById(R.id.btnQuiero)
        btnDeskartea = findViewById(R.id.btnDeskartea)
        layoutSelector = findViewById(R.id.layoutSelectorEnvido)

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
        // De
        // ntro de onCreate()

        layoutSelector = findViewById(R.id.layoutSelectorEnvido)
        val txtCantidad = findViewById<TextView>(R.id.txtCantidadEnvido)


        btnEnvidoMas.setOnClickListener {
            // Ocultamos la fila de botones principal
            btnPasar.visibility = View.GONE

            btnEnvidoMas.visibility = View.GONE
            btnQuiero.visibility = View.GONE

            // Mostramos el selector con animación
            layoutSelector.visibility = View.VISIBLE
            layoutSelector.alpha = 0f
            layoutSelector.animate()
                .alpha(1f)
                .setDuration(250)
                .start()

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

            findViewById<ProgressBar>(R.id.progressBottom).visibility = View.GONE

        }

        findViewById<Button>(R.id.btnOrdago).setOnClickListener {
            decisionContinuation?.resume("ordago", null)
            layoutSelector.visibility = View.GONE

             findViewById<ProgressBar>(R.id.progressBottom).visibility = View.GONE

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
        ocultarBarrasSistema()

        // --- LISTENERS DE LOS BOTONES CON R.ID ---
        btnMus.setOnClickListener {
            decisionContinuation?.resume("mus", null)

            findViewById<ProgressBar>(R.id.progressBottom).visibility = View.GONE
            ocultarTodo()

        }

        btnPasar.setOnClickListener {
            decisionContinuation?.resume("paso", null)
            findViewById<ProgressBar>(R.id.progressBottom).visibility = View.GONE
            ocultarTodo()


        }
        btnDeskartea.setOnClickListener {
            val discardString = buildDiscardString()
            decisionContinuation?.resume(discardString, null)

            findViewById<ProgressBar>(R.id.progressBottom).visibility = View.GONE
            ocultarTodo()

        }

        btnQuiero.setOnClickListener {
            decisionContinuation?.resume("quiero", null)
            ocultarTodo()
            findViewById<ProgressBar>(R.id.progressBottom).visibility = View.GONE

        }

// Dentro de onCreate
        apuestaRealizada = intent.getIntExtra("APUESTA_CANTIDAD", 0)

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

                val received = intent.getStringExtra(EXTRA_PARAM) ?: "PUBLICA"
                Log.e("PartidaActivity", "Enviando al servidor: $received")
                writer.write(received)
                writer.newLine()
                writer.flush()

                withContext(Dispatchers.Main) {
                    roundLabel.text = "BILATZEN..."
                }
                val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                val nireid = prefs.getString("userRegistrado", "") ?: ""

                if (intent.getStringExtra(EXTRA_PARAM) != "UNIRSE_PRIVADA") {
                    if (nireid.isNotEmpty()) {
                        writer.write(nireid)
                        writer.newLine()
                        writer.flush()
                    }
                }

                while (true) {
                    val serverMsg = reader.readLine() ?: break
                    withContext(Dispatchers.Main) {
                        findViewById<TextView>(R.id.txtRondaActual).text = "MUS"
                    }

                    when {
                        serverMsg.startsWith("ACTION:") -> {
                            val partes = serverMsg.split(":")
                            if (partes.size >= 3) {
                                withContext(Dispatchers.Main) {
                                    // Eliminado runOnUiThread innecesario porque ya estamos en Dispatchers.Main
                                    mostrarDecision(partes[1].toInt(), partes[2])
                                }
                            }
                        }

                        serverMsg.startsWith("TURN;") -> {
                            val partes = serverMsg.split(";")
                            if (partes.size >= 2) {
                                val uidConTurno = partes[1]
                                turnoID = uidConTurno
                                withContext(Dispatchers.Main) {
                                    ocultarTodasLasBarras()
                                    activarBarraPorID(uidConTurno)
                                }
                            }
                        }

                        serverMsg.startsWith("RONDA:") -> {
                            val ronda = serverMsg.substringAfter("RONDA:")
                            withContext(Dispatchers.Main) {
                                findViewById<MaterialCardView>(R.id.cardRondaCentral).visibility = View.VISIBLE
                                roundLabel.text = ronda
                                findViewById<TextView>(R.id.txtRondaActual).text = ronda
                            }
                        }

                        serverMsg.startsWith("LABURPENA:") -> {
                            val laburpena = serverMsg.substringAfter("LABURPENA:")
                            val partes = laburpena.split(",")
                            if (partes.size >= 3) {
                                val jokua = partes[0].trim()
                                val puntos = partes[1].trim()
                                val ganador = partes[2].trim().toIntOrNull() ?: 0

                                withContext(Dispatchers.Main) {
                                    actualizarMarcadorResumen(jokua, puntos, ganador)
                                }

                                if (jokua.uppercase().contains("JUEGO") || jokua.uppercase().contains("PUNTO") ||
                                    jokua.uppercase().contains("JOKUA") || jokua.uppercase().contains("PUNTUA")) {
                                    delay(5000)
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

                            withContext(Dispatchers.Main) {
                                if (miTalde == 1) {
                                    findViewById<TextView>(R.id.leftTeamName).text = "Etxekoak (NI)"
                                    findViewById<TextView>(R.id.rightTeamName).text = "Kanpokoak"
                                } else {
                                    findViewById<TextView>(R.id.leftTeamName).text = "Kanpokoak"
                                    findViewById<TextView>(R.id.rightTeamName).text = "Etxekoak (NI)"
                                }
                            }

                            val yo = listaEsperaJugadores.find { it.second == nireid }
                            if (yo != null) {
                                miTalde = yo.first
                            }
                        }

                        serverMsg == "CARDS" -> {
                            withContext(Dispatchers.Main) {
                                findViewById<MaterialCardView>(R.id.cardRondaCentral).visibility = View.VISIBLE
                                cobrarApuestaAlEmpezar()
                                findViewById<LinearLayout>(R.id.layoutSalaEspera).visibility = View.GONE
                                roundLabel.text = "BANATZEN"
                                goian.visibility = View.VISIBLE
                                behean.visibility = View.VISIBLE
                                eskuin.visibility = View.VISIBLE
                                ezker.visibility = View.VISIBLE
                            }
                            recibirCartas(reader, 4)
                        }

                        serverMsg == "TURN" -> {
                            withContext(Dispatchers.Main) {
                                toggleDecisionButtons(visible = true)
                                startTurnTimer("mus")
                                Toast.makeText(this@PartidaActivity, "Zure txanda!", Toast.LENGTH_SHORT).show()
                            }

                            val respuesta = suspendCancellableCoroutine<String> { cont ->
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
                                btnDeskartea.visibility = View.VISIBLE
                                startTurnTimer("0-1-2-3*")
                            }

                            val deskarteRespuesta = suspendCancellableCoroutine<String> { cont ->
                                decisionContinuation = cont
                            }

                            writer.write(deskarteRespuesta)
                            writer.newLine()
                            writer.flush()

                            withContext(Dispatchers.Main) {
                                limpiarCartasDescartadas()
                                btnDeskartea.visibility = View.GONE
                            }
                            recibirCartas(reader, 4)
                        }

                        serverMsg == "GRANDES" || serverMsg == "PEQUEÑAS" || serverMsg == "PARES" || serverMsg == "JUEGO" || serverMsg == "PUNTO" -> {
                            withContext(Dispatchers.Main) {
                                roundLabel.text = serverMsg
                                toggleEnvidoButtons(visible = true)
                                startTurnTimer("paso")
                                Toast.makeText(this@PartidaActivity, "$serverMsg jolasten!", Toast.LENGTH_SHORT).show()
                            }

                            val respuesta = suspendCancellableCoroutine<String> { cont ->
                                decisionContinuation = cont
                            }

                            writer.write(respuesta)
                            writer.newLine()
                            writer.flush()

                            // CORRECCIÓN CRÍTICA: Todo esto en Main para evitar el crash al cambiar de ronda
                            withContext(Dispatchers.Main) {
                                ocultarTodo()
                                // Aseguramos que los botones específicos también se oculten
                                btnQuiero.visibility = View.GONE
                                btnPasar.visibility = View.GONE
                            }
                            ordagoOn = false
                            envidoOn = false
                        }

                        serverMsg == "ORDAGO" -> ordagoOn = true
                        serverMsg == "ENVIDO" -> envidoOn = true

                        serverMsg == "PUNTUAKJASO" -> {
                            val l1 = reader.readLine()
                            val l2 = reader.readLine()
                            val r1 = reader.readLine()
                            val r2 = reader.readLine()
                            withContext(Dispatchers.Main) {
                                roundLabel.text = "Puntuazioa"
                                findViewById<TextView>(R.id.leftScoreBox1).text = l1
                                findViewById<TextView>(R.id.leftScoreBox2).text = l2
                                findViewById<TextView>(R.id.rightScoreBox1).text = r1
                                findViewById<TextView>(R.id.rightScoreBox2).text = r2
                            }
                        }

                        serverMsg.startsWith("CODIGO:") -> {
                            val kodea = serverMsg.substringAfter("CODIGO:")
                            withContext(Dispatchers.Main) {
                                findViewById<LinearLayout>(R.id.layoutSalaEspera).visibility = View.VISIBLE
                                findViewById<TextView>(R.id.txtCodigoPartida).text = kodea
                            }
                        }

                        serverMsg == "PEDIR_CODIGO" -> {
                            val kodea = intent.getStringExtra(EXTRA_CODE) ?: ""
                            writer.write(kodea)
                            writer.newLine()
                            writer.flush()
                            if (nireid.isNotEmpty()) {
                                writer.write(nireid)
                                writer.newLine()
                                writer.flush()
                            }
                        }

                        serverMsg == "ERABAKIA" -> {
                            val erabakia = reader.readLine() ?: ""
                            try {
                                val parts = erabakia.split(";", limit = 3)
                                if (parts.size >= 3) {
                                    val uid = parts[0].trim()
                                    val serverId = parts[1].trim().toIntOrNull() ?: 0
                                    var mensaje = parts[2].trim()

                                    when {
                                        mensaje.endsWith("PARES") -> {
                                            mensaje = if (mensaje.startsWith("jokuaDaukat")) "PARES DAUKAT" else "PARES EZ DUT"
                                        }
                                        mensaje.endsWith("JUEGO") -> {
                                            mensaje = if (mensaje.startsWith("jokuaDaukat")) "JUEGO DAUKAT" else "JUEGO EZ DUT"
                                        }
                                    }

                                    withContext(Dispatchers.Main) {
                                        showErabakiaPopup(serverId, "", mensaje)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("PartidaActivity", "Error ERABAKIA: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PartidaActivity, "Konexio errorea: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                socket?.close()
            }
        }
    }
    private fun ocultarBarrasSistema() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) ocultarBarrasSistema()
    }
    private fun cobrarApuestaAlEmpezar() {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val uid = prefs.getString("userRegistrado", "") ?: ""
        if (uid.isEmpty()) return

        val userRef = db.collection("Users").document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)

            // 1. Cobrar el dinero
            val dineroStr = snapshot.getString("dinero") ?: "0"
            val dineroActual = dineroStr.replace(",", ".").toDoubleOrNull() ?: 0.0
            val nuevoSaldo = dineroActual - apuestaRealizada.toDouble()
            val saldoFinalStr = "%.2f".format(java.util.Locale.US, nuevoSaldo)

            transaction.update(userRef, "dinero", saldoFinalStr)

            // 2. Sumar +1 a partidas totales (partidak)
            val partidasTotales = snapshot.getLong("partidak") ?: 0L
            transaction.update(userRef, "partidak", partidasTotales + 1)

            null
        }.addOnSuccessListener {
            Log.d("Firebase", "Apuesta cobrada y partida registrada.")
        }.addOnFailureListener { e ->
            Log.e("Firebase", "Errorea kobratzerakoan: ${e.message}")
        }
    }
    private fun actualizarMarcadorResumen(jokua: String, puntuazioa: String, ganadorRonda: Int) {
        val marcador = findViewById<MaterialCardView>(R.id.marcadorPro)
        findViewById<MaterialCardView>(R.id.cardRondaCentral).visibility = View.GONE

        // Convertir la puntuación a Int (asegurándote de que no falle si viene vacío)
        val puntosNuevos = puntuazioa.toIntOrNull() ?: 0

        // Sumar los puntos al total global del equipo ganador
        if (ganadorRonda == 1) {
            puntosEquipo1 += puntosNuevos
        } else if (ganadorRonda == 2) {
            puntosEquipo2 += puntosNuevos
        }

        // Actualizamos el marcador de arriba de 5 en 5 inmediatamente
        actualizarMarcadorPrincipal()

        // --- El resto de tu lógica para rellenar el marcadorPro ---
        val colorGanado = Color.parseColor("#4CAF50")
        val colorPerdido = Color.parseColor("#F44336")
        val colorResultado = if (ganadorRonda == miTalde) colorGanado else colorPerdido

        val fase = jokua.uppercase().trim()

        when (fase) {
            "GRANDES" -> {
                findViewById<TextView>(R.id.valHaundia).apply {
                    text = puntuazioa
                    setTextColor(colorResultado)
                }
            }
            "PEQUEÑAS" -> {
                findViewById<TextView>(R.id.valTxikia).apply {
                    text = puntuazioa
                    setTextColor(colorResultado)
                }
            }
            "PARES" -> {
                findViewById<TextView>(R.id.valPareak).apply {
                    text = puntuazioa
                    setTextColor(colorResultado)
                }
            }
            "JUEGO", "PUNTO", "JOKUA", "PUNTUA" -> {
                val labelJokua = findViewById<TextView>(R.id.labelJokua)
                val valJokua = findViewById<TextView>(R.id.valJokua)
                labelJokua.text = if (fase.contains("PUNT")) "PUNTUA" else "JOKUA"
                valJokua.text = puntuazioa
                valJokua.setTextColor(colorResultado)

                // Mostrar el resumen flotante (marcadorPro) durante 5 segundos
                marcador.visibility = View.VISIBLE
                marcador.alpha = 0f
                marcador.animate().alpha(1f).setDuration(500).start()

                marcador.postDelayed({
                    marcador.animate().alpha(0f).setDuration(500).withEndAction {
                        marcador.visibility = View.GONE
                    }
                }, 5000)
            }
        }
    }
    private fun actualizarMarcadorPrincipal() {
        // Cálculo para Equipo 1 (Etxekoak - Izquierda)
        val amarracos1 = puntosEquipo1 / 5
        val piedras1 = puntosEquipo1 % 5

        findViewById<TextView>(R.id.leftScoreBox1).text = amarracos1.toString()
        findViewById<TextView>(R.id.leftScoreBox2).text = piedras1.toString()

        // Cálculo para Equipo 2 (Kanpokoak - Derecha)
        val amarracos2 = puntosEquipo2 / 5
        val piedras2 = puntosEquipo2 % 5

        findViewById<TextView>(R.id.rightScoreBox1).text = amarracos2.toString()
        findViewById<TextView>(R.id.rightScoreBox2).text = piedras2.toString()

        // --- COMPROBACIÓN DE VICTORIA (40 puntos) ---
        if (puntosEquipo1 >= 40 || puntosEquipo2 >= 40) {
            findViewById<TextView>(R.id.leftScoreBox1).text = "0"
            findViewById<TextView>(R.id.leftScoreBox2).text = "0"
            findViewById<TextView>(R.id.rightScoreBox1).text = "0"
            findViewById<TextView>(R.id.rightScoreBox2).text = "0"
            val gureTaldeaIrabaziDu = if (puntosEquipo1 >= 40) miTalde == 1 else miTalde == 2

            // Pequeño delay para que el usuario vea los últimos puntos en el marcador
            Handler(Looper.getMainLooper()).postDelayed({
                mostrarDialogoFinal(gureTaldeaIrabaziDu)
            }, 1000)
        }
    }
    private fun resetearTextosMarcador() {
        findViewById<TextView>(R.id.valHaundia).text = ""
        findViewById<TextView>(R.id.valTxikia).text = ""
        findViewById<TextView>(R.id.valPareak).text = ""
        findViewById<TextView>(R.id.valJokua).text = ""
    }
    private fun mostrarDialogoFinal(ganaste: Boolean) {
        runOnUiThread {
            val dialog = android.app.Dialog(this)
            // Asegúrate de que el XML se llame exactamente así
            val view = layoutInflater.inflate(R.layout.dialog_resultado_final, null)
            dialog.setContentView(view)
            dialog.setCancelable(false)
            dialog.window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setDimAmount(0.90f)
            }

            val titulo = view.findViewById<TextView>(R.id.txtTituloFinal)
            val cuerpo = view.findViewById<TextView>(R.id.txtCuerpoFinal) // Asegúrate de que este ID existe en el XML
            val mensaje = view.findViewById<TextView>(R.id.txtMensajeFinal)
            val icono = view.findViewById<ImageView>(R.id.imgCopa)
            val btn = view.findViewById<Button>(R.id.btnIrtenFinal)

            if (ganaste) {
                titulo.text = "ZORIONAK!"
                titulo.setTextColor(Color.parseColor("#FFD700")) // Oro
                val premio = apuestaRealizada * 2
                cuerpo?.text = "IRABAZIA: +$premio €"
                mensaje.text = "Partida bikaina!\nBenetako txapeldunak zarete."
            } else {
                titulo.text = "GALDU DUZUE"
                titulo.setTextColor(Color.parseColor("#B0BEC5")) // Zilarra
                cuerpo?.text = "GALDURA: -$apuestaRealizada €"
                mensaje.text = "Gaur ez da zuen eguna izan.\nAnimo hurrengorako!"
                icono.apply {
                    alpha = 0.5f
                    rotation = 15f
                }
            }

            btn.setOnClickListener {
                actualizarDineroFirebase(ganaste)
                dialog.dismiss()
                var inent = Intent(this@PartidaActivity, MainActivity::class.java)
                startActivity(inent)
            }

            dialog.show()

            // Animación Pop-up
            view.alpha = 0f
            view.scaleX = 0.5f
            view.scaleY = 0.5f
            view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(700)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.5f))
                .start()
        }
    }

    private fun actualizarDineroFirebase(ganaste: Boolean) {
        if (!ganaste) return // Si ha perdido, el dinero ya se le quitó al empezar. No hacemos nada.

        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val uid = prefs.getString("userRegistrado", "") ?: ""
        if (uid.isEmpty()) return

        val userRef = db.collection("Users").document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)

            // 1. Leer saldo actual (que ya tiene la apuesta restada)
            val dineroStr = snapshot.getString("dinero") ?: "0"
            val dineroActual = dineroStr.replace(",", ".").toDoubleOrNull() ?: 0.0

            // 2. Sumar el premio (Apuesta original * 2)
            // Ejemplo: apostó 10, se le quitaron 10. Ahora gana 20 (recupera sus 10 + 10 del rival)
            val nuevoSaldo = dineroActual + (apuestaRealizada.toDouble() * 2)
            val saldoFinalStr = "%.2f".format(java.util.Locale.US, nuevoSaldo)

            // 3. Actualizar saldo y contador de victorias
            transaction.update(userRef, "dinero", saldoFinalStr)

            val victoriasActuales = snapshot.getLong("partidaIrabaziak") ?: 0L
            transaction.update(userRef, "partidaIrabaziak", victoriasActuales + 1)

            null
        }.addOnSuccessListener {
            Log.d("Firebase", "Saria emanda eta garaipena zenbatuta!")
        }
    }


    // --- CORRECCIÓN DE IDS DE VISTAS (MUY IMPORTANTE PARA EVITAR CRASH) ---


// MODIFICA EL BOTÓN DENTRO DE mostrarDialogoFinal

    private fun cuandoPosicion(serverId: Int): String {
        return when(serverId) {
            1 -> "Left"
            2 -> "Top"
            3 -> "Right"
            else -> "Bottom"
        }
    }

    private fun getAnchorIdForServerId(serverId: Int): Int {
        return when (serverId) {
            0 -> R.id.infoBottom  // <-- Asegúrate de que este ID sea exacto al del XML
            1 -> R.id.infoLeft
            2 -> R.id.infoTop
            3 -> R.id.infoRight
            else -> R.id.infoBottom
        }
    }
    private fun showErabakiaPopup(serverId: Int, uid: String, mensaje: String) {
        runOnUiThread {
            try {
                val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                val miId = prefs.getString("userRegistrado", "") ?: ""

                val yo = listaEsperaJugadores.find { it.second == miId }
                val miTurno = yo?.third ?: 0

                // 1. Determinar la posición relativa
                val posicion = when {
                    serverId == miTurno -> "Bottom"
                    serverId == (miTurno + 1) % 4 -> "Right"
                    serverId == (miTurno + 3) % 4 -> "Left"
                    else -> "Top"
                }

                // 2. Buscar el TextView correspondiente en el layout
                val viewId = resources.getIdentifier("status$posicion", "id", packageName)
                val statusTextView = findViewById<TextView>(viewId)

                statusTextView?.apply {
                    text = mensaje.uppercase()
                    visibility = View.VISIBLE
                    alpha = 0f
                    translationY = 30f // Efecto de subir

                    // Animación de entrada
                    animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .withEndAction {
                            // Se queda 3 segundos y desaparece
                            postDelayed({
                                animate()
                                    .alpha(0f)
                                    .translationY(-20f) // Efecto de seguir subiendo al irse
                                    .setDuration(400)
                                    .withEndAction { visibility = View.GONE }
                                    .start()
                            }, 3000)
                        }
                        .start()
                }
            } catch (e: Exception) {
                Log.e("PartidaActivity", "Error mostrando status: ${e.message}")
            }
        }
    }

    private var countDownTimer: CountDownTimer? = null

    private fun jokalarienInfo(taldea: Int, jokalariID: String, zerbitzariId: Int) {
        if (listaEsperaJugadores.none { it.second == jokalariID }) {
            listaEsperaJugadores.add(Triple(taldea, jokalariID, zerbitzariId))
        }

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

                    val posicion = when {
                        suId == miId -> "Bottom"
                        suTurno == turnoDerecha -> "Right"
                        suTurno == turnoIzquierda -> "Left"
                        else -> "Top"
                    }

                    cargarInfoEnVista(suId, posicion)


                }
            }
        }
    }

    private fun activarBarraPorID(idJugadorConTurno: String) {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val miId = prefs.getString("userRegistrado", "") ?: ""

        val yo = listaEsperaJugadores.find { it.second == miId }
        val el = listaEsperaJugadores.find { it.second == idJugadorConTurno }

        if (yo != null && el != null) {
            val miTurno = yo.third
            val suTurno = el.third

            val posicion = when {
                idJugadorConTurno == miId -> "Bottom"
                suTurno == (miTurno + 1) % 4 -> "Right"
                suTurno == (miTurno + 3) % 4 -> "Left"
                else -> "Top"
            }

            runOnUiThread {
                ocultarTodasLasBarras()
                activarTemporizador(posicion)
            }
        }
    }

    private fun ocultarTodasLasBarras() {
        countDownTimer?.cancel() // Detener el reloj actual

        val layoutIds = listOf(
            R.id.progressBottom,
            R.id.progressTop,
            R.id.progressLeft,
            R.id.progressRight
        )

        layoutIds.forEach { id ->
            val pb = findViewById<ProgressBar>(id)
            pb?.apply {
                visibility = View.INVISIBLE // O GONE si prefieres que no ocupe espacio
                progress = 0
            }
        }
    }

    private fun activarTemporizador(posicion: String) {
        // 1. Cancelar el timer anterior si existe para que no siga restando
        countDownTimer?.cancel()

        // 2. Lista de todas las progress bars para resetearlas
        val progressBars = listOf(
            findViewById<ProgressBar>(R.id.progressBottom),
            findViewById<ProgressBar>(R.id.progressTop),
            findViewById<ProgressBar>(R.id.progressLeft),
            findViewById<ProgressBar>(R.id.progressRight)
        )

        // 3. Ocultar TODAS y ponerlas a 0
        progressBars.forEach { pb ->
            pb?.visibility = View.INVISIBLE
            pb?.progress = 0
        }

        // 4. Identificar la barra actual
        val progressId = when (posicion) {
            "Bottom" -> R.id.progressBottom
            "Top" -> R.id.progressTop
            "Left" -> R.id.progressLeft
            "Right" -> R.id.progressRight
            else -> null
        }

        val pbActual = progressId?.let { findViewById<ProgressBar>(it) }

        // 5. Configurar y arrancar la barra del turno actual
        pbActual?.apply {
            visibility = View.VISIBLE
            progress = 100
            max = 100
        }

        countDownTimer = object : CountDownTimer(10000, 50) {
            override fun onTick(millisUntilFinished: Long) {
                val progreso = (millisUntilFinished.toFloat() / 10000 * 100).toInt()
                pbActual?.progress = progreso
            }
            override fun onFinish() {
                pbActual?.visibility = View.INVISIBLE
            }
        }.start()
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


            tv.alpha = 0f
            tv.scaleX = 0.5f
            tv.scaleY = 0.5f
            tv.animate()
                .alpha(1f)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(250)
                .withEndAction {
                    tv.animate().scaleX(1f).scaleY(1f).setDuration(100)
                }

            tv.postDelayed({
                tv.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction { tv.visibility = View.GONE }
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
    private fun ocultarTodo() {
        runOnUiThread {
            btnMus.visibility = View.GONE
            btnPasar.visibility = View.GONE

            btnEnvidoMas.visibility = View.GONE
            btnQuiero.visibility = View.GONE
            btnDeskartea.visibility = View.GONE
            layoutSelector.visibility = View.GONE
            findViewById<ProgressBar>(R.id.progressBottom).visibility = View.GONE
        }
    }
    private fun toggleDecisionButtons(visible: Boolean) {
        runOnUiThread {
            val estado = if (visible) View.VISIBLE else View.GONE
            btnMus.visibility = estado
            btnPasar.visibility = estado
            // Asegurarnos de que el resto estén ocultos
            if (visible) {

                btnEnvidoMas.visibility = View.GONE
                btnQuiero.visibility = View.GONE
                layoutSelector.visibility = View.GONE
            }
        }
    }

    private fun toggleEnvidoButtons(visible: Boolean) {
        runOnUiThread {
            if (!visible) {

                btnEnvidoMas.visibility = View.GONE
                btnPasar.visibility = View.GONE
                btnQuiero.visibility = View.GONE
                layoutSelector.visibility = View.GONE
            } else {
                btnPasar.visibility = View.VISIBLE


                btnEnvidoMas.visibility = View.VISIBLE

                btnQuiero.visibility = if (envidoOn || ordagoOn) View.VISIBLE else View.GONE

                btnMus.visibility = View.GONE
            }
        }
    }

    private var gameTimer: android.os.CountDownTimer? = null

    private fun startTurnTimer(autoResponse: String) {
        gameTimer?.cancel()
        val progressBar = findViewById<ProgressBar>(R.id.progressBottom)
        progressBar.visibility = View.VISIBLE

        gameTimer = object : android.os.CountDownTimer(10000, 100) {
            override fun onTick(millisUntilFinished: Long) {
                progressBar.progress = (millisUntilFinished / 100).toInt()
            }

            override fun onFinish() {
                progressBar.visibility = View.GONE
                decisionContinuation?.let {
                    if (it.isActive) {
                        it.resume(autoResponse, null)
                    }
                }
            }
        }.start()
    }
    private fun buildDiscardString(): String {
        if (selectedIndices.isEmpty()) return "*"
        val seleccionadas = selectedIndices.map { currentCards[it] }
        return seleccionadas.joinToString("-") + "*"
    }
}