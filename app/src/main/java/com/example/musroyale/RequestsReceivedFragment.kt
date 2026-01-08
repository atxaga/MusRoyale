package com.example.musroyale

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class RequestsReceivedFragment : Fragment() {
    private var adapter: FriendsAdapter? = null
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_requests_received, container, false)
        val recyclerView = root.findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inicializamos con lista vacía y modo RECIBIDAS
        adapter = FriendsAdapter(mutableListOf(), "RECIBIDAS")
        recyclerView.adapter = adapter

        // Iniciamos la escucha en tiempo real
        escucharSolicitudesRecibidas()

        return root
    }

    private fun escucharSolicitudesRecibidas() {
        val prefs = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentUserEmail = prefs.getString("userRegistrado", null) ?: return

        // Escuchamos cambios en MI documento
        listenerRegistration = db.collection("Users").document(currentUserEmail)
            .addSnapshotListener { document, error ->
                if (error != null) return@addSnapshotListener

                if (document != null && document.exists()) {
                    // Obtenemos la lista de IDs de solicitudes
                    val solicitudesIds = document.get("solicitudRecivida") as? List<String> ?: listOf()

                    if (solicitudesIds.isNotEmpty()) {
                        // Si hay IDs, buscamos sus datos (nombre, etc.)
                        db.collection("Users")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), solicitudesIds)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                val listaParaAdapter = mutableListOf<Map<String, String>>()
                                for (doc in querySnapshot.documents) {
                                    listaParaAdapter.add(mapOf(
                                        "id" to doc.id,
                                        "username" to (doc.getString("username") ?: "Usuario")
                                    ))
                                }
                                adapter?.updateData(listaParaAdapter)
                            }
                    } else {
                        // Si la lista está vacía en Firebase, limpiamos el adapter
                        adapter?.updateData(mutableListOf())
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // MUY IMPORTANTE: Detener el listener al salir para evitar consumo de datos
        listenerRegistration?.remove()
    }
}
