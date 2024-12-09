package com.example.ideality.utils

import android.content.Context
import android.widget.Toast
import com.example.ideality.models.Product
import com.google.firebase.database.FirebaseDatabase

object TestDataUtility {
    fun addTestProductsToFirebase(context: Context) {
        val database = FirebaseDatabase.getInstance()
        val productsRef = database.getReference("products")

        val testProducts = listOf(
            Product(
                id = "sofa_black",
                name = "Elegant Black Sofa",
                description = "Contemporary black sofa with premium leather upholstery. Perfect blend of comfort and sophistication for modern living spaces.",
                price = 799.99,
                thumbnailUrl = "https://cloud.appwrite.io/v1/storage/buckets/67530358002e27d51581/files/6756eb14000078d25450/view?project=6738a00700041c6e85ca&project=6738a00700041c6e85ca&mode=admin",
                modelUrl = "https://cloud.appwrite.io/v1/storage/buckets/6752f4800003bd835e3d/files/6756eaa80013f65b1e41/view?project=6738a00700041c6e85ca&project=6738a00700041c6e85ca&mode=admin",
                modelId = "6756eaa80013f65b1e41",
                category = "sofa",
                rating = 4.7f,
                reviewCount = 85,
                isNew = true,
                dimensions = mapOf(
                    "length" to 220.0,
                    "width" to 95.0,
                    "height" to 80.0
                )
            ),
            Product(
                id = "sofa_white_set",
                name = "Luxurious White Sofa Set",
                description = "Premium white sofa set featuring clean lines and plush cushions. Includes a three-seater sofa and two matching armchairs.",
                price = 1499.99,
                thumbnailUrl = "https://cloud.appwrite.io/v1/storage/buckets/67530358002e27d51581/files/6756ea680008011bca14/view?project=6738a00700041c6e85ca&project=6738a00700041c6e85ca&mode=admin",
                modelUrl = "https://cloud.appwrite.io/v1/storage/buckets/6752f4800003bd835e3d/files/6756e9a8001fb3a3715c/view?project=6738a00700041c6e85ca&project=6738a00700041c6e85ca&mode=admin",
                modelId = "6756e9a8001fb3a3715c",
                category = "sofa_set",
                rating = 4.8f,
                reviewCount = 62,
                isNew = true,
                dimensions = mapOf(
                    "length" to 280.0,
                    "width" to 98.0,
                    "height" to 85.0
                )
            )
        )

        var successCount = 0
        var failureCount = 0

        testProducts.forEach { product ->
            productsRef.child(product.id).setValue(product.toMap())
                .addOnSuccessListener {
                    successCount++
                    if (successCount + failureCount == testProducts.size) {
                        showCompletionToast(context, successCount, failureCount)
                    }
                }
                .addOnFailureListener { e ->
                    failureCount++
                    if (successCount + failureCount == testProducts.size) {
                        showCompletionToast(context, successCount, failureCount)
                    }
                }
        }
    }

    private fun showCompletionToast(context: Context, successCount: Int, failureCount: Int) {
        val message = when {
            failureCount == 0 -> "All $successCount products added successfully!"
            successCount == 0 -> "Failed to add any products!"
            else -> "$successCount products added, $failureCount failed"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}