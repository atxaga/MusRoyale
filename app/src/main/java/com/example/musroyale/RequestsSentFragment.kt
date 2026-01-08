package com.example.musroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.content.Context.MODE_PRIVATE
import com.google.firebase.firestore.FirebaseFirestore

class RequestsSentFragment : Fragment() {
    private var adapter: FriendsAdapter? = null
    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_requests_sent, container, false)
        val recyclerView = root.findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inicializamos el adapter con modo ENVIADAS
        adapter = FriendsAdapter(mutableListOf(), modo = "ENVIADAS")
        recyclerView.adapter = adapter

        // Iniciamos la escucha
        escucharSolicitudesEnviadas()

        return root
    }

    private fun escucharSolicitudesEnviadas() {
        val prefs = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentUserEmail = prefs.getString("userRegistrado", null) ?: return

        // Escucha en tiempo real de MI documento
        listenerRegistration = db.collection("Users").document(currentUserEmail)
            .addSnapshotListener { document, error ->
                if (error != null) return@addSnapshotListener

                if (document != null && document.exists()) {
                    val enviadasIds = document.get("solicitudMandada") as? List<String> ?: listOf()

                    // Limpiamos IDs vacíos
                    val idsValidos = enviadasIds.filter { it.isNotBlank() }

                    if (idsValidos.isNotEmpty()) {
                        // Buscamos los datos de los usuarios a los que enviamos solicitud
                        db.collection("Users")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), idsValidos)
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
                        // Si no hay enviadas, vaciamos el adapter
                        adapter?.updateData(mutableListOf())
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Liberar el listener para evitar fugas de memoria y consumo innecesario
        listenerRegistration?.remove()
    }
}