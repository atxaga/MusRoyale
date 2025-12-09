package com.example.musroyale

import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.musroyale.databinding.FragmentFooterBinding



class FooterFragment: Fragment() {
    private lateinit var binding: FragmentFooterBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFooterBinding.inflate(inflater, container, false)


        return binding.root
    }
}