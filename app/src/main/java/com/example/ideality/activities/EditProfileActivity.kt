package com.example.ideality.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import io.appwrite.models.InputFile
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.ideality.R
import com.example.ideality.databinding.ActivityEditProfileBinding
import com.example.ideality.models.UserData
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.yalantis.ucrop.UCrop
import io.appwrite.Client
import io.appwrite.services.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var client: Client
    private lateinit var storage: Storage

    private var currentUser: UserData? = null
    private var tempImageUri: Uri? = null
    private var isProfileChanged = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                startCrop(uri)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            tempImageUri?.let { uri ->
                startCrop(uri)
            }
        }
    }

    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                isProfileChanged = true
                tempImageUri = uri
                loadProfileImage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFirebase()
        initializeAppwrite()
        setupClickListeners()
        loadUserData()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
    }

    private fun initializeAppwrite() {
        client = Client(this)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("6738a00700041c6e85ca") // Replace with your project ID
        storage = Storage(client)
    }

    private fun setupClickListeners() {
        binding.apply {
            // Back button
            backButton.setOnClickListener {
                onBackPressed()
            }

            // Profile image
            changePhotoButton.setOnClickListener {
                showImagePickerDialog()
            }
            profileImage.setOnClickListener {
                showImagePickerDialog()
            }

            // Edit buttons
            editEmailButton.setOnClickListener {
                startActivity(Intent(this@EditProfileActivity, EditEmailActivity::class.java))
            }
            editPhoneButton.setOnClickListener {
                startActivity(Intent(this@EditProfileActivity, EditPhoneActivity::class.java))
            }

            // Save button
            saveButton.setOnClickListener {
                if (validateInputs()) {
                    saveUserData()
                }
            }
        }
    }

    private fun loadUserData() {
        showLoading(true)
        val userId = auth.currentUser?.uid ?: return

        database.getReference("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUser = snapshot.getValue(UserData::class.java)
                    currentUser?.let { user ->
                        binding.apply {
                            usernameInput.setText(user.username)
                            emailText.text = user.email
                            phoneText.text = user.phone.ifEmpty { "Add phone number" }

                            // Load profile image if available
                            if (user.profileImageId.isNotEmpty()) {
                                lifecycleScope.launch {
                                    try {
                                        val previewUrl = storage.getFilePreview(
                                            bucketId = "6738a492002ac3c28a0d", // Replace with your bucket ID
                                            fileId = user.profileImageId
                                        ).toString()

                                        Glide.with(this@EditProfileActivity)
                                            .load(previewUrl)
                                            .apply(RequestOptions.circleCropTransform())
                                            .placeholder(R.drawable.ic_profile)
                                            .error(R.drawable.ic_profile)
                                            .into(profileImage)
                                    } catch (e: Exception) {
                                        showError("Error loading profile image")
                                    }
                                }
                            }
                        }
                    }
                    showLoading(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoading(false)
                    showError("Failed to load user data")
                }
            })
    }

    private fun showImagePickerDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_image_picker, null)

        view.findViewById<View>(R.id.cameraOption).setOnClickListener {
            dialog.dismiss()
            checkCameraPermission()
        }

        view.findViewById<View>(R.id.galleryOption).setOnClickListener {
            dialog.dismiss()
            openGallery()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val photoFile = File.createTempFile(
            "IMG_",
            ".jpg",
            getExternalFilesDir(null)
        )
        tempImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            photoFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, tempImageUri)
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_${UUID.randomUUID()}.jpg"))

        val options = UCrop.Options().apply {
            setCircleDimmedLayer(true)
            setShowCropFrame(true)
            setShowCropGrid(true)
            setCompressionQuality(80)
        }

        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1080, 1080)
            .withOptions(options)
            .let { cropLauncher.launch(it.getIntent(this)) }
    }

    private fun loadProfileImage(image: Any) {
        Glide.with(this)
            .load(image)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(binding.profileImage)
    }

    private fun validateInputs(): Boolean {
        val username = binding.usernameInput.text.toString().trim()

        return when {
            username.isEmpty() -> {
                binding.usernameInput.error = "Username is required"
                false
            }
            username.length < 3 -> {
                binding.usernameInput.error = "Username must be at least 3 characters"
                false
            }
            !username.matches("[a-zA-Z0-9._-]+".toRegex()) -> {
                binding.usernameInput.error = "Username can only contain letters, numbers, dots, underscores and hyphens"
                false
            }
            else -> true
        }
    }

    private fun saveUserData() {
        showLoading(true)
        val userId = auth.currentUser?.uid ?: return
        val username = binding.usernameInput.text.toString().trim()

        // Check if username exists (if changed)
        if (username != currentUser?.username) {
            database.getReference("users")
                .orderByChild("username")
                .equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            showLoading(false)
                            binding.usernameInput.error = "Username already exists"
                        } else {
                            uploadProfileImageIfNeeded(userId, username)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showLoading(false)
                        showError("Failed to check username")
                    }
                })
        } else {
            uploadProfileImageIfNeeded(userId, username)
        }
    }

    private fun uploadProfileImageIfNeeded(userId: String, username: String) {
        if (isProfileChanged && tempImageUri != null) {
            lifecycleScope.launch {
                try {
                    // Create a temporary file from Uri
                    val inputStream = contentResolver.openInputStream(tempImageUri!!)
                    val file = File(cacheDir, "temp_profile_image.jpg")
                    file.outputStream().use { outputStream ->
                        inputStream?.copyTo(outputStream)
                    }

                    // Delete existing profile image if any
                    currentUser?.profileImageId?.let { oldImageId ->
                        try {
                            storage.deleteFile(
                                bucketId = "6738a492002ac3c28a0d",
                                fileId = oldImageId
                            )
                        } catch (e: Exception) {
                            // Ignore if file doesn't exist
                        }
                    }

                    // Convert File to InputFile for Appwrite
                    val inputFile = InputFile.fromFile(file)

                    // Upload new image
                    val result = storage.createFile(
                        bucketId = "6738a492002ac3c28a0d",
                        fileId = "unique()",
                        file = inputFile
                    )

                    // Update user data with new image ID
                    withContext(Dispatchers.Main) {
                        updateUserData(userId, username, result.id)
                    }

                    // Clean up temporary file
                    file.delete()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        showError("Failed to upload profile image: ${e.message}")
                    }
                }
            }
        } else {
            updateUserData(userId, username, currentUser?.profileImageId ?: "")
        }
    }

    private fun updateUserData(userId: String, username: String, profileImageId: String) {
        val updates = hashMapOf<String, Any>(
            "username" to username,
            "profileImageId" to profileImageId
        )

        database.getReference("users").child(userId)
            .updateChildren(updates)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    showSuccess("Profile updated successfully")
                    finish()
                } else {
                    showError("Failed to update profile")
                }
            }
    }

    private fun showLoading(show: Boolean) {
        binding.loadingProgress.visibility = if (show) View.VISIBLE else View.GONE
        binding.saveButton.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}