package com.example.musroyale

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

// CAMBIO CLAVE: Ahora recibe 'fragment: Fragment' en lugar de FragmentActivity
class FriendsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    // Mantener instancias para la búsqueda (Nota: Esto funciona para listas pequeñas y fijas)
    private val fragments: List<Fragment> = listOf(
        FriendsListFragment(),
        RequestsSentFragment(),
        RequestsReceivedFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        // Devolvemos la instancia de nuestra lista
        return fragments[position]
    }

    // Método utilitario para obtener la instancia para el buscador
    fun getFragmentAt(position: Int): Fragment {
        return fragments[position]
    }
}