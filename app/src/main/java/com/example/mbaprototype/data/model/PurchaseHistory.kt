package com.example.mbaprototype.data.model

import java.util.Date

// Represents a past purchase record
data class PurchaseHistory(
    val purchaseId: String,
    val purchaseDate: Date,
    val items: List<Product> // List of products bought in this purchase
)