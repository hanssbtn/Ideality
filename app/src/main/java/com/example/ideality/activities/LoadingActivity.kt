package com.example.ideality.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.ideality.databinding.ActivityLoadingBinding

class LoadingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    companion object {
        fun show(context: Context) {
            context.startActivity(Intent(context, LoadingActivity::class.java))
        }

        fun hide(activity: Activity) {
            activity.finish()
        }
    }
}