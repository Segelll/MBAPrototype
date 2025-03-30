package com.example.mbaprototype.data

import com.example.mbaprototype.data.model.BasketItem
import com.example.mbaprototype.data.model.Category
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.data.model.PurchaseHistory
import java.util.Date
import java.util.UUID

object DataSource {

    val categories = listOf(
        Category(id = "cat1", name = "Fruits"),
        Category(id = "cat2", name = "Dairy"),
        Category(id = "cat3", name = "Bakery")
    )

    val products = listOf(
        Product(
            id = "prod1", name = "Organic Apples", categoryId = "cat1", price = 3.50,
            ingredients = listOf("Apple")
        ),
        Product(
            id = "prod2", name = "Whole Milk (1L)", categoryId = "cat2", price = 1.80,
            ingredients = listOf("Milk", "Vitamin D")
        ),
        Product(
            id = "prod3", name = "Sourdough Bread", categoryId = "cat3", price = 4.20,
            ingredients = listOf("Flour", "Water", "Salt", "Sourdough Starter")
        ),
        Product(
            id = "prod4", name = "Cheddar Cheese (200g)", categoryId = "cat2", price = 3.10,
            ingredients = listOf("Milk", "Salt", "Cheese Cultures", "Rennet")
        )
    )

    val pastPurchases = listOf(
        PurchaseHistory(
            purchaseId = UUID.randomUUID().toString(),
            purchaseDate = Date(System.currentTimeMillis() - 86400000 * 7),
            items = listOf(
                BasketItem(products[1], 2),
                BasketItem(products[2], 1)
            )
        ),
        PurchaseHistory(
            purchaseId = UUID.randomUUID().toString(),
            purchaseDate = Date(System.currentTimeMillis() - 86400000 * 2),
            items = listOf(
                BasketItem(products[0], 5)
            )
        )
    )

    fun getProductById(id: String): Product? {
        return products.find { it.id == id }
    }

    fun getCategoryById(id: String): Category? {
        return categories.find { it.id == id }
    }

    fun getProductsByCategory(): Map<Category, List<Product>> {
        return products.groupBy { product ->
            categories.find { it.id == product.categoryId }!!
        }
    }
}