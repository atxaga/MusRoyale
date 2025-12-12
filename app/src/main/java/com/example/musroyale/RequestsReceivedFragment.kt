package com.example.musroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RequestsReceivedFragment : Fragment() {
    private lateinit var adapter: FriendsAdapter

    private val sampleData = mutableListOf(
        "Lucia",
        "Marta"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_requests_received, container, false)
        val recyclerView = root.findViewById<RecyclerView>(R.id.recyclerView)

        adapter = FriendsAdapter(sampleData.toMutableList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        return root
    }
}
