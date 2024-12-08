package com.example.ideality.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ideality.databinding.AddEditAddressLayoutBinding
import com.example.ideality.models.Address
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

class AddEditAddressActivity : AppCompatActivity() {
    private lateinit var binding: AddEditAddressLayoutBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var isProcessing = false
    private var hasChanges = false
    private var addressId: String? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddEditAddressLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFirebase()
        setupUI()
        setupClickListeners()
        setupTextWatchers()

        // Check if we're editing an existing address
        addressId = intent.getStringExtra("addressId")
        isEditMode = addressId != null

        if (isEditMode) {
            binding.toolbarTitle.text = "Edit Address"
            loadExistingAddress()
        } else {
            binding.toolbarTitle.text = "Add New Address"
        }
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
    }

    private fun setupUI() {
        binding.apply {
            // Clear any initial errors
            addressLabelLayout.error = null
            fullNameLayout.error = null
            phoneLayout.error = null
            streetAddressLayout.error = null
            cityLayout.error = null
            postalCodeLayout.error = null
            stateLayout.error = null

            // Setup state dropdown
            val states = resources.getStringArray(com.example.ideality.R.array.states)
            stateInput.setAdapter(android.widget.ArrayAdapter(
                this@AddEditAddressActivity,
                android.R.layout.simple_dropdown_item_1line,
                states
            ))
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            backButton.setOnClickListener {
                onBackPressedHandler()
            }

            saveButton.setOnClickListener {
                if (!isProcessing && validateInputs()) {
                    saveAddress()
                }
            }
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                hasChanges = true
                updateSaveButton()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.apply {
            addressLabelInput.addTextChangedListener(textWatcher)
            fullNameInput.addTextChangedListener(textWatcher)
            phoneInput.addTextChangedListener(textWatcher)
            streetAddressInput.addTextChangedListener(textWatcher)
            cityInput.addTextChangedListener(textWatcher)
            postalCodeInput.addTextChangedListener(textWatcher)
            stateInput.addTextChangedListener(textWatcher)
            additionalInfoInput.addTextChangedListener(textWatcher)
        }
    }

    private fun loadExistingAddress() {
        showLoading(true)
        val userId = auth.currentUser?.uid ?: return

        database.getReference("users")
            .child(userId)
            .child("addresses")
            .child(addressId!!)
            .get()
            .addOnSuccessListener { snapshot ->
                val map = snapshot.value as? Map<String, Any?> ?: return@addOnSuccessListener
                val address = Address.fromMap(snapshot.key ?: "", map)

                binding.apply {
                    addressLabelInput.setText(address.label)
                    fullNameInput.setText(address.fullName)
                    phoneInput.setText(address.phoneNumber)
                    streetAddressInput.setText(address.streetAddress)
                    cityInput.setText(address.city)
                    postalCodeInput.setText(address.postalCode)
                    stateInput.setText(address.state)
                    additionalInfoInput.setText(address.additionalInfo)
                    setAsDefaultCheckbox.isChecked = address.isDefault
                }

                hasChanges = false
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showError("Failed to load address: ${e.message}")
                showLoading(false)
                finish()
            }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        binding.apply {
            // Reset errors
            addressLabelLayout.error = null
            fullNameLayout.error = null
            phoneLayout.error = null
            streetAddressLayout.error = null
            cityLayout.error = null
            postalCodeLayout.error = null
            stateLayout.error = null

            // Validate each field
            if (addressLabelInput.text.isNullOrBlank()) {
                addressLabelLayout.error = "Please enter an address label"
                isValid = false
            }

            if (fullNameInput.text.isNullOrBlank()) {
                fullNameLayout.error = "Please enter full name"
                isValid = false
            }

            if (phoneInput.text.isNullOrBlank()) {
                phoneLayout.error = "Please enter phone number"
                isValid = false
            } else if (!android.util.Patterns.PHONE.matcher(phoneInput.text).matches()) {
                phoneLayout.error = "Please enter a valid phone number"
                isValid = false
            }

            if (streetAddressInput.text.isNullOrBlank()) {
                streetAddressLayout.error = "Please enter street address"
                isValid = false
            }

            if (cityInput.text.isNullOrBlank()) {
                cityLayout.error = "Please enter city"
                isValid = false
            }

            if (postalCodeInput.text.isNullOrBlank()) {
                postalCodeLayout.error = "Please enter postal code"
                isValid = false
            }

            if (stateInput.text.isNullOrBlank()) {
                stateLayout.error = "Please select state"
                isValid = false
            }
        }
        return isValid
    }

    private fun saveAddress() {
        isProcessing = true
        showLoading(true)

        val userId = auth.currentUser?.uid ?: return
        val addressRef = database.getReference("users")
            .child(userId)
            .child("addresses")

        // Create address object
        val address = Address(
            id = addressId ?: UUID.randomUUID().toString(),
            label = binding.addressLabelInput.text.toString(),
            fullName = binding.fullNameInput.text.toString(),
            phoneNumber = binding.phoneInput.text.toString(),
            streetAddress = binding.streetAddressInput.text.toString(),
            city = binding.cityInput.text.toString(),
            state = binding.stateInput.text.toString(),
            postalCode = binding.postalCodeInput.text.toString(),
            additionalInfo = binding.additionalInfoInput.text.toString(),
            isDefault = binding.setAsDefaultCheckbox.isChecked
        )

        // If setting as default, remove default status from other addresses
        if (address.isDefault) {
            addressRef.get().addOnSuccessListener { snapshot ->
                snapshot.children.forEach { child ->
                    if (child.key != address.id &&
                        child.child("isDefault").getValue(Boolean::class.java) == true) {
                        child.ref.child("isDefault").setValue(false)
                    }
                }
                completeAddressSave(addressRef, address)
            }.addOnFailureListener { e ->
                showError("Failed to update addresses: ${e.message}")
                isProcessing = false
                showLoading(false)
            }
        } else {
            completeAddressSave(addressRef, address)
        }
    }

    private fun completeAddressSave(addressRef: com.google.firebase.database.DatabaseReference, address: Address) {
        addressRef.child(address.id)
            .setValue(address.toMap())
            .addOnSuccessListener {
                showSuccess(if (isEditMode) "Address updated successfully" else "Address added successfully")
                finish()
            }
            .addOnFailureListener { e ->
                showError("Failed to save address: ${e.message}")
                isProcessing = false
                showLoading(false)
            }
    }

    private fun updateSaveButton() {
        binding.saveButton.isEnabled = hasChanges && !isProcessing
    }

    private fun onBackPressedHandler() {
        if (isProcessing) {
            showError("Please wait while processing...")
            return
        }

        if (hasChanges) {
            showExitConfirmationDialog()
        } else {
            finish()
        }
    }

    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Discard Changes")
            .setMessage("Are you sure you want to discard your changes?")
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            loadingBackground.visibility = if (show) View.VISIBLE else View.GONE
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
            saveButton.isEnabled = !show && hasChanges
            // Disable all inputs while loading
            addressLabelInput.isEnabled = !show
            fullNameInput.isEnabled = !show
            phoneInput.isEnabled = !show
            streetAddressInput.isEnabled = !show
            cityInput.isEnabled = !show
            postalCodeInput.isEnabled = !show
            stateInput.isEnabled = !show
            additionalInfoInput.isEnabled = !show
            setAsDefaultCheckbox.isEnabled = !show
            backButton.isEnabled = !show
        }
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSuccess(message: String) {
        if (!isFinishing) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        onBackPressedHandler()
    }
}