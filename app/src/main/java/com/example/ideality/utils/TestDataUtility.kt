package com.example.ideality.utils

import android.content.Context
import android.widget.Toast
import com.example.ideality.models.Product
import com.google.firebase.database.FirebaseDatabase

object TestDataUtility {
    fun addTestProductToFirebase(context: Context) {
        val database = FirebaseDatabase.getInstance()
        val productsRef = database.getReference("products")

        val testProduct = Product(
            id = "sofa1",
            name = "Modern Sofa",
            description = "Luxurious modern sofa perfect for your living room. Features comfortable cushions and durable fabric.",
            price = 599.99,
            thumbnailUrl = "https://cloud.appwrite.io/v1/storage/buckets/67530358002e27d51581/files/67530364000a05737f6c/view?project=6738a00700041c6e85ca&project=6738a00700041c6e85ca&mode=admin",
            modelUrl = "https://cloud.appwrite.io/v1/storage/buckets/6752f4800003bd835e3d/files/6752fa1e001da2814f94/view?project=6738a00700041c6e85ca&project=6738a00700041c6e85ca&mode=admin",
            modelId = "6752fa1e001da2814f94",
            category = "sofa",
            rating = 4.5f,
            reviewCount = 128,
            isNew = true,
            dimensions = mapOf(
                "length" to 200.0,
                "width" to 90.0,
                "height" to 85.0
            )
        )

        productsRef.child(testProduct.id).setValue(testProduct.toMap())
            .addOnSuccessListener {
                Toast.makeText(context, "Test product added successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error adding test product: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}