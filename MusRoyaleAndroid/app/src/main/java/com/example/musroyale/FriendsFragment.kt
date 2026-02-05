package com.example.musroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class FriendsFragment : Fragment() {

    // Si decides usar ViewBinding más adelante, descomenta e implementa aquí
    // private var _binding: FragmentFriendsBinding? = null
    // private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflamos el layout. Asegúrate de que el XML se llame 'activity_friends' o 'fragment_friends'
        return inflater.inflate(R.layout.fragment_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // IMPORTANTE: En Fragments buscamos las vistas usando 'view.findViewById'
        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)
        val headerTabLayout = view.findViewById<TabLayout>(R.id.header_tab_layout)
        val searchView = view.findViewById<androidx.appcompat.widget.SearchView>(R.id.search_view)

        // Pasamos 'this' (el Fragmento) al adaptador.
        // Asegúrate de que tu FriendsPagerAdapter acepte un Fragment en el constructor.
        val adapter = FriendsPagerAdapter(this)
        viewPager.adapter = adapter

        val tabTitles = arrayOf("Lagunak", "Bidaliak", "Jasoak")
        TabLayoutMediator(headerTabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Llama a la función de búsqueda en el fragmento actual del ViewPager
                (adapter.getFragmentAt(viewPager.currentItem) as? Searchable)?.onSearch(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Búsqueda en tiempo real
                (adapter.getFragmentAt(viewPager.currentItem) as? Searchable)?.onSearch(newText ?: "")
                return true
            }
        })
    }
}

// La interfaz se queda igual, puede estar en este archivo o en uno separado
interface Searchable {
    fun onSearch(query: String)
}
