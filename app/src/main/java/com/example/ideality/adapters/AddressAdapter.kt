package com.example.ideality.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.ideality.R
import com.example.ideality.databinding.AddressItemLayoutBinding
import com.example.ideality.models.Address
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddressAdapter(
    private val onEditClick: (Address) -> Unit,
    private val onDeleteClick: (Address) -> Unit,
    private val onSetDefaultClick: (Address) -> Unit
) : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    private var addresses: List<Address> = emptyList()

    fun updateAddresses(newAddresses: List<Address>) {
        addresses = newAddresses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val binding = AddressItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AddressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(addresses[position])
    }

    override fun getItemCount(): Int = addresses.size

    inner class AddressViewHolder(
        private val binding: AddressItemLayoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(address: Address) {
            binding.apply {
                addressLabel.text = address.label
                fullAddress.text = buildString {
                    append(address.streetAddress)
                    append("\n")
                    append("${address.city}, ${address.state} ${address.postalCode}")
                }
                phoneNumber.text = address.phoneNumber

                // Show/hide default chip
                defaultChip.isVisible = address.isDefault

                // Handle click events
                editButton.setOnClickListener { onEditClick(address) }
                deleteButton.setOnClickListener {
                    showDeleteDialog(address)
                }

                setDefaultButton.apply {
                    isVisible = !address.isDefault
                    setOnClickListener { onSetDefaultClick(address) }
                }
            }
        }

        private fun showDeleteDialog(address: Address) {
            val dialog = MaterialAlertDialogBuilder(binding.root.context, R.style.AlertDialog_App_Custom)
                .setView(R.layout.dialog_delete_address)
                .create()

            dialog.show()

            // Handle button clicks
            dialog.findViewById<MaterialButton>(R.id.btnCancel)?.setOnClickListener {
                dialog.dismiss()
            }

            dialog.findViewById<MaterialButton>(R.id.btnDelete)?.setOnClickListener {
                dialog.dismiss()
                onDeleteClick(address)
            }
        }
    }
}