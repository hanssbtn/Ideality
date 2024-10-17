package com.example.ideality.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.ideality.R

class HomeFragment: Fragment() {
    private val tag = "HomeFragment"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(tag, "OnCreateView: Home Fragment Layout inflated.")
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val previewButton: ImageButton = view.findViewById(R.id.preview_button)
        val catalogueButton: ImageButton = view.findViewById(R.id.catalogue_button)
        val ordersButton: ImageButton = view.findViewById(R.id.orders_button)
        val modelsButton: ImageButton = view.findViewById(R.id.models_button)
        Log.d(tag, "OnViewCreated: previewButton id = ${previewButton.id}")
        Log.d(tag, "OnViewCreated: ordersButton id = ${ordersButton.id}")
        Log.d(tag, "OnViewCreated: catalogueButton id = ${catalogueButton.id}")
        Log.d(tag, "OnViewCreated: modelsButton id = ${modelsButton.id}")


        Log.d(tag, "OnViewCreated: Setting OnClickListeners")
        previewButton?.setOnClickListener { v ->
            findNavController().navigate(R.id.action_homeFragment_to_previewFragment)
        }
        ordersButton?.setOnClickListener { v ->
            findNavController().navigate(R.id.action_homeFragment_to_previewFragment)
        }
        modelsButton?.setOnClickListener { v ->
            findNavController().navigate(R.id.action_homeFragment_to_previewFragment)
        }
        catalogueButton?.setOnClickListener { v ->
            findNavController().navigate(R.id.action_homeFragment_to_previewFragment)
        }
    }
}