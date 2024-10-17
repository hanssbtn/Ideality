package com.example.ideality.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ideality.R

class LoginFragment: Fragment() {
    private val taG = "LoginFragment"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(taG, "OnCreateView: creating view.")
        val v = inflater.inflate(R.layout.fragment_login, container, false)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(taG, "OnViewCreated: Created LoginFragment")


    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}