package com.example.musroyale

import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.musroyale.databinding.FragmentHeaderBinding
import com.example.musroyale.databinding.FragmentSartuBinding


class HeaderFragment: Fragment() {
    private lateinit var binding: FragmentHeaderBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHeaderBinding.inflate(inflater, container, false)

        binding.btnAddBalance.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            val intent = Intent(act, AddBalanceActivity::class.java)
            act.startActivity(intent)
        }

        return binding.root
    }
}