package com.example.mbaprototype.data.model

// Represents an item currently in the shopping basket (Product + quantity)
// Although not strictly required by the prompt, it's good practice for baskets
data class BasketItem(
    val product: Product,
    var quantity: Int = 1 // Default quantity is 1
)