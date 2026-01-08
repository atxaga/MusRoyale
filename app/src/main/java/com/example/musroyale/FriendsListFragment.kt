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

class FriendsListFragment : Fragment(), Searchable {
    private lateinit var adapter: FriendsAdapter
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_friends_list, container, false)
        val recyclerView = root.findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Iniciamos la escucha en tiempo real
        escucharCambiosAmigos(recyclerView)

        return root
    }

    private fun escucharCambiosAmigos(recyclerView: RecyclerView) {
        val prefs = requireActivity().getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentUserEmail = prefs.getString("userRegistrado", null) ?: return

        // 1. Escuchamos mi propio documento (para saber quiénes son mis amigos y mis enviadas)
        listenerRegistration = db.collection("Users").document(currentUserEmail)
            .addSnapshotListener { myDoc, error ->
                if (error != null || myDoc == null) return@addSnapshotListener

                val misAmigosIds = myDoc.get("amigos") as? List<String> ?: listOf()
                val misSolicitudesIds = myDoc.get("solicitudMandada") as? List<String> ?: listOf()

                // 2. Cada vez que mis listas cambien, refrescamos la lista global de usuarios
                // Nota: También puedes poner un listener en la colección entera si quieres ver nuevos usuarios al instante
                db.collection("Users").get().addOnSuccessListener { allUsersQuery ->
                    val listaAmigos = mutableListOf<Map<String, String>>()
                    val listaResto = mutableListOf<Map<String, String>>()

                    for (doc in allUsersQuery.documents) {
                        val id = doc.id
                        if (id == currentUserEmail) continue

                        val userData = mutableMapOf(
                            "id" to id,
                            "username" to (doc.getString("username") ?: "Sin nombre")
                        )

                        when {
                            misAmigosIds.contains(id) -> {
                                userData["relacion"] = "AMIGO"
                                listaAmigos.add(userData)
                            }
                            misSolicitudesIds.contains(id) -> {
                                userData["relacion"] = "PENDIENTE"
                                listaResto.add(userData)
                            }
                            else -> {
                                userData["relacion"] = "EXPLORAR"
                                listaResto.add(userData)
                            }
                        }
                    }

                    val listaFinal = mutableListOf<Map<String, String>>()
                    listaFinal.addAll(listaAmigos)
                    listaFinal.addAll(listaResto)

                    // 3. Actualizamos el adapter
                    if (::adapter.isInitialized) {
                        adapter.updateData(listaFinal)
                    } else {
                        adapter = FriendsAdapter(listaFinal, "BUSCAR")
                        recyclerView.adapter = adapter
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Importante: Cerramos el listener al destruir la vista para no gastar recursos
        listenerRegistration?.remove()
    }

    override fun onSearch(query: String) {
        if (::adapter.isInitialized) {
            adapter.filter(query)
        }
    }
}