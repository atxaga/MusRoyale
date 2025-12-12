package com.example.musroyale

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class FriendsPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    // Mantener instancias simples para poder pasar búsquedas
    private val fragments: List<Fragment> = listOf(
        FriendsListFragment(),
        RequestsSentFragment(),
        RequestsReceivedFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    // Método utilitario para obtener la instancia
    fun getFragmentAt(position: Int): Fragment = fragments[position]
}

