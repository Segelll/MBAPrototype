package com.example.mbaprototype.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val id: String,
    val name: String,
    val categoryId: String,
    val price: Double,
    val imageUrl: String? = null,
    val ingredients: List<String>? = null
) : Parcelable