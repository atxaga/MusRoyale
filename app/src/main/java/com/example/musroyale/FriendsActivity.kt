package com.example.musroyale

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class FriendsActivity : AppCompatActivity() {
    // private lateinit var binding: ActivityFriendsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val headerTabLayout = findViewById<TabLayout>(R.id.header_tab_layout)
        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.search_view)

        val adapter = FriendsPagerAdapter(this)
        viewPager.adapter = adapter

        val tabTitles = arrayOf("Amigos", "Enviadas", "Recibidas")
        TabLayoutMediator(headerTabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                (adapter.getFragmentAt(viewPager.currentItem) as? Searchable)?.onSearch(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                (adapter.getFragmentAt(viewPager.currentItem) as? Searchable)?.onSearch(newText ?: "")
                return true
            }
        })
    }
}

interface Searchable {
    fun onSearch(query: String)
}
