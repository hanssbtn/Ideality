/*package com.example.ideality.managers

// managers/ModelManager.kt
class ModelManager(private val storage: FirebaseStorage = FirebaseStorage.getInstance()) {
    private val modelsRef = storage.reference.child("models")

    fun uploadModel(
        modelUri: Uri,
        productId: String,
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val modelRef = modelsRef.child("$productId.glb")

        val uploadTask = modelRef.putFile(modelUri)

        uploadTask
            .addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                onProgress(progress)
            }
            .addOnSuccessListener {
                // Get download URL after successful upload
                modelRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun getModelUrl(
        productId: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        modelsRef.child("$productId.glb")
            .downloadUrl
            .addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}*/