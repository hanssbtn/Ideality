package com.example.ideality.activities

import android.os.Bundle
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ideality.R
import com.hbb20.CountryCodePicker

class SignUp : AppCompatActivity() {
    private lateinit var ccp: CountryCodePicker
    private lateinit var phoneInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize views
        ccp = findViewById(R.id.ccp)
        phoneInput = findViewById(R.id.phoneInput)

        // Register the phone number field with CCP
        ccp.registerCarrierNumberEditText(phoneInput)


        }
    }
