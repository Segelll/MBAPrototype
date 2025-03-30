package com.example.mbaprototype.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Represents a product. Implement Parcelable to pass it between Activities/Fragments.
@Parcelize
data class Product(
    val id: String,
    val name: String,
    val categoryId: String, // Links to Category
    val price: Double,
    val imageUrl: String? = null, // Placeholder for image URL
    val ingredients: List<String>? = null // Optional list of ingredients
) : Parcelable