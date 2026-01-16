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

        // Obtenemos la referencia al overlay de carga que está en el XML de la Activity
        val loadingOverlay = requireActivity().findViewById<View>(R.id.loadingOverlay)

        listenerRegistration = db.collection("Users").document(currentUserEmail)
            .addSnapshotListener { myDoc, error ->
                if (error != null || myDoc == null) {
                    loadingOverlay?.visibility = View.GONE
                    return@addSnapshotListener
                }

                val misAmigosIds = myDoc.get("amigos") as? List<String> ?: listOf()
                val misSolicitudesIds = myDoc.get("solicitudMandada") as? List<String> ?: listOf()

                db.collection("Users").get().addOnSuccessListener { allUsersQuery ->
                    val listaAmigos = mutableListOf<Map<String, String>>()
                    val listaResto = mutableListOf<Map<String, String>>()

                    // ... dentro de escucharCambiosAmigos, en el addOnSuccessListener:
                    for (doc in allUsersQuery.documents) {
                        val id = doc.id
                        if (id == currentUserEmail) continue

                        val userData = mutableMapOf(
                            "id" to id,
                            "username" to (doc.getString("username") ?: "Sin nombre"),
                            "avatarActual" to (doc.getString("avatarActual") ?: "avatar_default.png"),
                            "premium" to (doc.getBoolean("premium") ?: false).toString() // Incluimos premium
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

// Lista total que guardará el adaptador internamente
                    val listaTotal = mutableListOf<Map<String, String>>()
                    listaTotal.addAll(listaAmigos)
                    listaTotal.addAll(listaResto)

                    if (::adapter.isInitialized) {
                        adapter.updateData(listaTotal)
                    } else {
                        // Al crear el adaptador por primera vez, le pasamos la lista total
                        adapter = FriendsAdapter(listaTotal, "BUSCAR")
                        recyclerView.adapter = adapter

                        // IMPORTANTE: Llamamos al filtro vacío para que aplique la lógica de "ocultar no-amigos" al inicio
                        adapter.filter("")
                    }

                    // --- AQUÍ SE OCULTA ---
                    loadingOverlay?.visibility = View.GONE

                }.addOnFailureListener {
                    loadingOverlay?.visibility = View.GONE
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