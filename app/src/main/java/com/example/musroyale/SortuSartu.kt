package com.example.musroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.musroyale.databinding.FragmentSartuBinding
import com.example.musroyale.databinding.FragmentSortuBinding

class SortuFragment : Fragment(R.layout.fragment_sortu) {
    private lateinit var binding: FragmentSortuBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSortuBinding.inflate(inflater, container, false)


        return binding.root
    }
}

class SartuFragment : Fragment(R.layout.fragment_sartu) {
    private lateinit var binding: FragmentSartuBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSartuBinding.inflate(inflater, container, false)


        return binding.root
    }}
