package com.example.mbaprototype.data

import android.content.Context
import android.util.Log
import com.example.mbaprototype.data.model.BasketItem
import com.example.mbaprototype.data.model.Category
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.data.model.PurchaseHistory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Date
import java.util.UUID

object DataSource {

    lateinit var products: List<Product>
        private set
    lateinit var categories: List<Category>
        private set

    // pastPurchases, ürün listesi yüklendikten sonra initialize edilmeli.
    // Bu yüzden lateinit var yapıp, initProductsAndCategories sonrası initialize edeceğiz.
    lateinit var pastPurchases: List<PurchaseHistory>
        private set


    fun init(context: Context) {
        if (::products.isInitialized && ::categories.isInitialized) {
            // Veri zaten yüklenmişse tekrar yükleme
            return
        }
        loadProductsAndCategoriesFromCsv(context, "urunler.csv")
        initializePastPurchases()
    }

    private fun loadProductsAndCategoriesFromCsv(context: Context, fileName: String) {
        val tempProducts = mutableListOf<Product>()
        val tempCategoryMap = mutableMapOf<String, String>() // categoryName to categoryId
        var categoryCounter = 1

        try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    reader.readLine() // Başlık satırını atla (productId,ad,icerik,kategori)

                    while (reader.readLine().also { line = it } != null) {
                        val tokens = line!!.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                            .map { it.trim().removeSurrounding("\"") }

                        if (tokens.size >= 4) {
                            val productId = tokens[0]
                            val ad = tokens[1]
                            val icerik = tokens[2]
                            val kategori = tokens[3]

                            val categoryId = tempCategoryMap.getOrPut(kategori) {
                                "cat${categoryCounter++}"
                            }

                            tempProducts.add(
                                Product(
                                    id = productId,
                                    name = ad,
                                    categoryId = categoryId,
                                    price = null, // Fiyat bilgisi CSV'de yok ve gösterilmeyecek
                                    ingredients = parseIngredients(icerik)
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("DataSource", "Error reading CSV file: $fileName", e)
            // Hata durumunda boş listelerle devam et veya bir hata mekanizması kullan
            tempProducts.clear()
            tempCategoryMap.clear()
        }

        products = tempProducts.toList()
        categories = tempCategoryMap.map { (name, id) -> Category(id = id, name = name) }.toList()

        if (products.isEmpty()) {
            Log.w("DataSource", "No products loaded from CSV. Product list is empty.")
        }
        if (categories.isEmpty()) {
            Log.w("DataSource", "No categories derived from CSV. Category list is empty.")
        }
    }

    private fun parseIngredients(ingredientsString: String?): List<String> {
        return ingredientsString?.split('/')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }

    private fun initializePastPurchases() {
        // products listesi yüklendikten sonra pastPurchases'ı initialize et
        pastPurchases = if (products.size >= 2) {
            listOf(
                PurchaseHistory(
                    purchaseId = UUID.randomUUID().toString(),
                    purchaseDate = Date(System.currentTimeMillis() - 86400000 * 7), // 7 days ago
                    items = listOf(
                        BasketItem(products[0], 2),
                        BasketItem(products[1], 1)
                    )
                ),
                PurchaseHistory(
                    purchaseId = UUID.randomUUID().toString(),
                    purchaseDate = Date(System.currentTimeMillis() - 86400000 * 2), // 2 days ago
                    items = if (products.size >= 3) listOf(BasketItem(products[2], 5)) else emptyList()
                )
            )
        } else {
            emptyList()
        }
    }


    fun getProductById(id: String): Product? {
        if (!::products.isInitialized) return null
        return products.find { it.id == id }
    }

    fun getCategoryById(id: String): Category? {
        if (!::categories.isInitialized) return null
        return categories.find { it.id == id }
    }

    fun getProductsByCategory(): Map<Category, List<Product>> {
        if (!::products.isInitialized || !::categories.isInitialized) return emptyMap()
        return products.groupBy { product ->
            categories.find { it.id == product.categoryId }!!
        }
    }
}