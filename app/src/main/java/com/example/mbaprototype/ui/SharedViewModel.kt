package com.example.mbaprototype.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mbaprototype.data.DataSource
import com.example.mbaprototype.data.model.BasketItem
import com.example.mbaprototype.data.model.Category
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.data.model.PurchaseHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

sealed interface ProductListItem {
    data class CategoryHeader(val category: Category) : ProductListItem
    data class ProductItem(val product: Product) : ProductListItem
}


class SharedViewModel : ViewModel() {

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    private val _allCategories = MutableStateFlow<List<Category>>(emptyList())
    private val _filteredProductFlow = MutableStateFlow<String?>(null)

    val categorizedProductList: StateFlow<List<ProductListItem>> = _filteredProductFlow.map { query ->
        val productsToCategorize = if (query.isNullOrBlank()) {
            _allProducts.value
        } else {
            _allProducts.value.filter {
                it.name.contains(query, ignoreCase = true) ||
                        DataSource.getCategoryById(it.categoryId)?.name?.contains(query, ignoreCase = true) == true
            }
        }
        productsToCategorize.groupBy { product ->
            _allCategories.value.find { it.id == product.categoryId }
        }.filterKeys { it != null }
            .flatMap { (category, productList) ->
                listOf(ProductListItem.CategoryHeader(category!!)) + productList.map { ProductListItem.ProductItem(it) }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private val _basketItems = MutableStateFlow<List<BasketItem>>(emptyList())
    val basketItems: StateFlow<List<BasketItem>> = _basketItems.asStateFlow()

    val basketTotalCost: StateFlow<Double> = _basketItems.map { items ->
        items.sumOf { it.product.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)


    private val _favoriteProducts = MutableStateFlow<Set<String>>(emptySet())
    val favoriteProducts: StateFlow<Set<String>> = _favoriteProducts.asStateFlow()

    private val _pastPurchases = MutableStateFlow<List<PurchaseHistory>>(emptyList())
    val pastPurchases: StateFlow<List<PurchaseHistory>> = _pastPurchases.asStateFlow()

    private val _clickedProductIds = MutableStateFlow<Set<String>>(emptySet())
    val clickedProductIds: StateFlow<Set<String>> = _clickedProductIds.asStateFlow()

    private val _recommendations = MutableStateFlow<List<Product>>(emptyList())
    val recommendations: StateFlow<List<Product>> = _recommendations.asStateFlow()


    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _allProducts.value = DataSource.products
            _allCategories.value = DataSource.categories
            _pastPurchases.value = DataSource.pastPurchases
            _filteredProductFlow.value = null
        }
    }

    fun getBasketItemQuantity(productId: String): Int {
        return _basketItems.value.find { it.product.id == productId }?.quantity ?: 0
    }


    fun addProductToBasket(product: Product) {
        viewModelScope.launch {
            _basketItems.update { currentList ->
                val existingItemIndex = currentList.indexOfFirst { it.product.id == product.id }
                if (existingItemIndex != -1) {
                    val updatedItem = currentList[existingItemIndex].copy(
                        quantity = currentList[existingItemIndex].quantity + 1
                    )
                    currentList.toMutableList().apply { set(existingItemIndex, updatedItem) }.toList()
                } else {
                    currentList + BasketItem(product = product, quantity = 1)
                }
            }
            updateRecommendations()
        }
    }

    fun decreaseBasketQuantity(productId: String) {
        viewModelScope.launch {
            _basketItems.update { currentList ->
                val existingItemIndex = currentList.indexOfFirst { it.product.id == productId }
                if (existingItemIndex != -1) {
                    val currentItem = currentList[existingItemIndex]
                    if (currentItem.quantity > 1) {
                        val updatedItem = currentItem.copy(quantity = currentItem.quantity - 1)
                        currentList.toMutableList().apply { set(existingItemIndex, updatedItem) }.toList()
                    } else {
                        currentList.filterNot { it.product.id == productId }
                    }
                } else {
                    currentList
                }
            }
            updateRecommendations()
        }
    }

    fun removeProductFromBasket(productId: String) {
        viewModelScope.launch {
            _basketItems.update { currentList ->
                currentList.filterNot { it.product.id == productId }
            }
            updateRecommendations()
        }
    }

    fun toggleFavorite(productId: String) {
        viewModelScope.launch {
            _favoriteProducts.update { currentFavorites ->
                if (currentFavorites.contains(productId)) {
                    currentFavorites - productId
                } else {
                    currentFavorites + productId
                }
            }
        }
    }

    fun trackProductClick(productId: String) {
        viewModelScope.launch {
            _clickedProductIds.update { currentClicks ->
                (currentClicks + productId).toList().takeLast(20).toSet()
            }
        }
    }

    fun searchProducts(query: String) {
        _filteredProductFlow.value = query
    }

    private fun updateRecommendations() {
        viewModelScope.launch {
            val basketIds = _basketItems.value.map { it.product.id }.toSet()
            val allProds = DataSource.products
            val potentialRecs = allProds.filterNot { product ->
                basketIds.contains(product.id)
            }
            val mockRecs = potentialRecs.take(3)
            _recommendations.value = mockRecs
        }
    }

    fun completePurchase(): Boolean {
        val currentBasket = _basketItems.value
        if (currentBasket.isEmpty()) {
            return false
        }
        viewModelScope.launch {
            val historyRecord = PurchaseHistory(
                purchaseId = UUID.randomUUID().toString(),
                purchaseDate = Date(),
                items = currentBasket
            )
            _pastPurchases.update { currentHistory -> listOf(historyRecord) + currentHistory }
            _basketItems.value = emptyList()
            updateRecommendations()
        }
        return true
    }

    fun getProductById(productId: String): Product? {
        return DataSource.products.find { it.id == productId }
    }

    fun getPurchaseById(id: String): PurchaseHistory? {
        Log.d("SharedViewModel", "Searching for Purchase ID: $id")
        Log.d("SharedViewModel", "Current _pastPurchases size: ${_pastPurchases.value.size}")
        _pastPurchases.value.forEachIndexed { index, history ->
            Log.d("SharedViewModel", "History[$index] ID: ${history.purchaseId}")
        }
        val found = _pastPurchases.value.find { it.purchaseId == id }
        Log.d("SharedViewModel", "Find result for ID $id: ${if (found != null) "FOUND" else "NOT FOUND"}")
        return found
    }

    fun isFavorite(productId: String): Boolean {
        return _favoriteProducts.value.contains(productId)
    }

    fun isInBasket(productId: String): Boolean {
        return _basketItems.value.any { it.product.id == productId }
    }
}