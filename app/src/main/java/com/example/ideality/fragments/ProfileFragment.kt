package com.example.ideality.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ideality.R
import com.example.ideality.databinding.FragmentProfileBinding
import com.example.ideality.models.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.ideality.activities.ContactUsActivity
import com.example.ideality.activities.EditProfileActivity
import com.example.ideality.activities.FaqActivity
import com.example.ideality.activities.Home
import com.example.ideality.activities.PrivacyPolicyActivity
import com.example.ideality.activities.SettingsActivity
import com.example.ideality.activities.ShippingAddressActivity
import com.example.ideality.activities.TermsConditionsActivity
import com.example.ideality.activities.LogIn
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.appwrite.Client
import io.appwrite.services.Storage
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val currentUser = auth.currentUser

    // Appwrite client
    private lateinit var client: Client
    private lateinit var storage: Storage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the main toolbar when profile is shown
        (requireActivity() as? Home)?.let { homeActivity ->
            homeActivity.findViewById<View>(R.id.toolbar)?.visibility = View.GONE
        }

        val collapsingToolbar = binding.collapsingToolbar
        collapsingToolbar.title = "Profile"

        initializeAppwrite()
        setupUI()
        loadUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Show the main toolbar again when leaving profile
        (requireActivity() as? Home)?.let { homeActivity ->
            homeActivity.findViewById<View>(R.id.toolbar)?.visibility = View.VISIBLE
        }
        _binding = null
    }

    private fun initializeAppwrite() {
        client = Client(requireContext())
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("6738a00700041c6e85ca")
        storage = Storage(client)
    }

    private fun setupUI() {
        binding.apply {
            // Edit Profile
            editProfileButton.setOnClickListener {
                startActivity(Intent(requireContext(), EditProfileActivity::class.java))
            }

            // Account Section
            addressLayout.setOnClickListener {
                startActivity(Intent(requireContext(), ShippingAddressActivity::class.java))
            }

            settingsLayout.setOnClickListener {
                startActivity(Intent(requireContext(), SettingsActivity::class.java))
            }

            // Help & Support Section
            faqLayout.setOnClickListener {
                startActivity(Intent(requireContext(), FaqActivity::class.java))
            }

            contactLayout.setOnClickListener {
                startActivity(Intent(requireContext(), ContactUsActivity::class.java))
            }

            privacyPolicyLayout.setOnClickListener {
                startActivity(Intent(requireContext(), PrivacyPolicyActivity::class.java))
            }

            termsLayout.setOnClickListener {
                startActivity(Intent(requireContext(), TermsConditionsActivity::class.java))
            }

            // Logout
            logoutButton.setOnClickListener {
                showLogoutConfirmationDialog()
            }
        }
    }

    private fun loadUserData() {
        currentUser?.let { user ->
            val userRef = database.getReference("users").child(user.uid)

            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userData = snapshot.getValue(UserData::class.java)
                    userData?.let { updateUI(it) }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error loading profile", Toast.LENGTH_SHORT).show()
                }
            })
        } ?: run {
            navigateToLogin()
        }
    }

    private fun updateUI(userData: UserData) {
        binding.apply {
            usernameText.text = userData.username
            phoneNumber.text = userData.phone.ifEmpty { "Add phone number" }

            // Load profile image if available
            if (userData.profileImageId.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        // Get the URL as a string
                        val previewUrl = storage.getFilePreview(
                            bucketId = "6738a492002ac3c28a0d",
                            fileId = userData.profileImageId
                        ).toString()

                        // Load the image using Glide
                        Glide.with(this@ProfileFragment)
                            .load(previewUrl)  // Now passing a string URL
                            .apply(RequestOptions.circleCropTransform())
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(profileImage)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error loading profile image", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Set default profile image
                profileImage.setImageResource(R.drawable.ic_profile)
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout_confirmation, null)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
            .setView(dialogView)
            .create()

        // Find views in custom layout
        dialogView.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            dialog.dismiss()
            logout()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun logout() {
        // Clear local data
        requireContext().getSharedPreferences("app_prefs", 0).edit().clear().apply()

        // Sign out from Firebase
        auth.signOut()

        // Navigate to login
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(),  LogIn::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }



    companion object {
        fun newInstance() = ProfileFragment()
    }
}