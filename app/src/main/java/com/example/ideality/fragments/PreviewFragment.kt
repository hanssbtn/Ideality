package com.example.ideality.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ideality.R
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.node.ModelNode

class PreviewFragment: Fragment() {

    private lateinit var arSceneView: ARSceneView
    var modelNode : ModelNode? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_preview, container, false)
    }
}