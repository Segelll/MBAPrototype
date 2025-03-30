package com.example.mbaprototype.data

import com.example.mbaprototype.data.model.Category
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.data.model.PurchaseHistory
import java.util.Date
import java.util.UUID

// Simulates a remote or local data source
object DataSource {

    val categories = listOf(
        Category(id = "cat1", name = "Fruits"),
        Category(id = "cat2", name = "Dairy"),
        Category(id = "cat3", name = "Bakery")
    )

    val products = listOf(
        Product(
            id = "prod1",
            name = "Organic Apples",
            categoryId = "cat1",
            price = 3.50,
            ingredients = listOf("Apple"),
            imageUrl = null // Add image URLs later if needed
        ),
        Product(
            id = "prod2",
            name = "Whole Milk (1L)",
            categoryId = "cat2",
            price = 1.80,
            ingredients = listOf("Milk", "Vitamin D"),
            imageUrl = null
        ),
        Product(
            id = "prod3",
            name = "Sourdough Bread",
            categoryId = "cat3",
            price = 4.20,
            ingredients = listOf("Flour", "Water", "Salt", "Sourdough Starter"),
            imageUrl = null
        ),
        Product(
            id = "prod4",
            name = "Cheddar Cheese (200g)",
            categoryId = "cat2",
            price = 3.10,
            ingredients = listOf("Milk", "Salt", "Cheese Cultures", "Rennet"),
            imageUrl = null
        )
        // Add more products as needed
    )

    // Mock past purchases
    val pastPurchases = listOf(
        PurchaseHistory(
            purchaseId = UUID.randomUUID().toString(),
            purchaseDate = Date(System.currentTimeMillis() - 86400000 * 7), // 7 days ago
            items = listOf(products[1], products[2]) // Milk and Bread
        ),
        PurchaseHistory(
            purchaseId = UUID.randomUUID().toString(),
            purchaseDate = Date(System.currentTimeMillis() - 86400000 * 2), // 2 days ago
            items = listOf(products[0]) // Apples
        )
    )

    fun getProductById(id: String): Product? {
        return products.find { it.id == id }
    }

    fun getCategoryById(id: String): Category? {
        return categories.find { it.id == id }
    }
}