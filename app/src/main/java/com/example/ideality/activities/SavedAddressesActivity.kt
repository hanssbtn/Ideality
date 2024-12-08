package com.example.ideality.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ideality.adapter.AddressAdapter
import com.example.ideality.databinding.SavedAddressesLayoutBinding
import com.example.ideality.models.Address
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SavedAddressesActivity : AppCompatActivity() {
    private lateinit var binding: SavedAddressesLayoutBinding
    private lateinit var addressAdapter: AddressAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SavedAddressesLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFirebase()
        setupRecyclerView()
        setupClickListeners()
        loadAddresses()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
    }

    private fun setupRecyclerView() {
        addressAdapter = AddressAdapter(
            onEditClick = { address ->
                if (!isProcessing) {
                    startAddEditAddress(address)
                }
            },
            onDeleteClick = { address ->
                if (!isProcessing) {
                    deleteAddress(address)
                }
            },
            onSetDefaultClick = { address ->
                if (!isProcessing) {
                    setDefaultAddress(address)
                }
            }
        )

        binding.addressesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SavedAddressesActivity)
            adapter = addressAdapter
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            backButton.setOnClickListener {
                if (!isProcessing) {
                    finish()
                }
            }

            addAddressButton.setOnClickListener {
                if (!isProcessing) {
                    startAddEditAddress(null)
                }
            }
        }
    }

    private fun loadAddresses() {
        showLoading(true)
        val userId = auth.currentUser?.uid ?: return

        database.getReference("users")
            .child(userId)
            .child("addresses")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val addresses = mutableListOf<Address>()

                    snapshot.children.forEach { child ->
                        val map = child.value as? Map<String, Any?> ?: return@forEach
                        addresses.add(Address.fromMap(child.key ?: "", map))
                    }

                    addresses.sortByDescending { it.isDefault }
                    addressAdapter.updateAddresses(addresses)

                    // Update UI based on whether we have addresses
                    updateEmptyState(addresses.isEmpty())
                    showLoading(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Failed to load addresses: ${error.message}")
                    showLoading(false)
                }
            })
    }

    private fun deleteAddress(address: Address) {
        isProcessing = true
        showLoading(true)

        val userId = auth.currentUser?.uid ?: return

        database.getReference("users")
            .child(userId)
            .child("addresses")
            .child(address.id)
            .removeValue()
            .addOnSuccessListener {
                showSuccess("Address deleted successfully")
                // If we deleted the default address and there are other addresses,
                // we'll need to set a new default
                if (address.isDefault) {
                    setNewDefaultAddressIfNeeded()
                }
                isProcessing = false
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showError("Failed to delete address: ${e.message}")
                isProcessing = false
                showLoading(false)
            }
    }

    private fun setDefaultAddress(address: Address) {
        isProcessing = true
        showLoading(true)

        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(userId).child("addresses")

        // First, remove default status from all addresses
        userRef.get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { child ->
                if (child.child("isDefault").getValue(Boolean::class.java) == true) {
                    child.ref.child("isDefault").setValue(false)
                }
            }

            // Then set the new default address
            userRef.child(address.id)
                .child("isDefault")
                .setValue(true)
                .addOnSuccessListener {
                    showSuccess("Default address updated")
                    isProcessing = false
                    showLoading(false)
                }
                .addOnFailureListener { e ->
                    showError("Failed to update default address: ${e.message}")
                    isProcessing = false
                    showLoading(false)
                }
        }.addOnFailureListener { e ->
            showError("Failed to update addresses: ${e.message}")
            isProcessing = false
            showLoading(false)
        }
    }

    private fun setNewDefaultAddressIfNeeded() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(userId).child("addresses")

        userRef.limitToFirst(1).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val firstAddress = snapshot.children.first()
                firstAddress.ref.child("isDefault").setValue(true)
            }
        }
    }

    private fun startAddEditAddress(address: Address?) {
        val intent = Intent(this, AddEditAddressActivity::class.java)
        address?.let {
            intent.putExtra("addressId", it.id)
        }
        startActivity(intent)
        overridePendingTransition(com.example.ideality.R.anim.slide_in_right, com.example.ideality.R.anim.slide_out_left)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
            addressesRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            loadingBackground.visibility = if (show) View.VISIBLE else View.GONE
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
            addAddressButton.isEnabled = !show
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
        if (!isProcessing) {
            super.onBackPressed()
        }
    }
}