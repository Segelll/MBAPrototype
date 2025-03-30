package com.example.mbaprototype.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mbaprototype.data.DataSource
import com.example.mbaprototype.data.model.BasketItem
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.data.model.PurchaseHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SharedViewModel : ViewModel() {

    // --- All Products (for display and filtering) ---
    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts.asStateFlow()

    // --- Filtered Products (based on search) ---
    private val _filteredProducts = MutableStateFlow<List<Product>>(emptyList())
    val filteredProducts: StateFlow<List<Product>> = _filteredProducts.asStateFlow()

    // --- Basket ---
    private val _basketItems = MutableStateFlow<List<BasketItem>>(emptyList())
    val basketItems: StateFlow<List<BasketItem>> = _basketItems.asStateFlow()

    // --- Favorites ---
    private val _favoriteProducts = MutableStateFlow<Set<String>>(emptySet()) // Store IDs for efficiency
    val favoriteProducts: StateFlow<Set<String>> = _favoriteProducts.asStateFlow()

    // --- Past Purchases ---
    private val _pastPurchases = MutableStateFlow<List<PurchaseHistory>>(emptyList())
    val pastPurchases: StateFlow<List<PurchaseHistory>> = _pastPurchases.asStateFlow()

    // --- Clicked Products (simple tracking for potential AI input) ---
    private val _clickedProductIds = MutableStateFlow<Set<String>>(emptySet())
    val clickedProductIds: StateFlow<Set<String>> = _clickedProductIds.asStateFlow()

    // --- Recommendations (Placeholder) ---
    private val _recommendations = MutableStateFlow<List<Product>>(emptyList())
    val recommendations: StateFlow<List<Product>> = _recommendations.asStateFlow()


    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch { // Use viewModelScope for lifecycle awareness
            _allProducts.value = DataSource.products
            _filteredProducts.value = DataSource.products // Initially show all
            _pastPurchases.value = DataSource.pastPurchases
            // Load initial favorites if any (e.g., from saved preferences - mock for now)
            // _favoriteProducts.value = setOf("prod1") // Example: Apple favorited initially
        }
    }

    fun addProductToBasket(product: Product) {
        viewModelScope.launch {
            _basketItems.update { currentList ->
                val existingItem = currentList.find { it.product.id == product.id }
                if (existingItem != null) {
                    // If item exists, update its quantity (optional, for now just replace/ensure it's there)
                    currentList // Or implement quantity update if needed: currentList.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
                } else {
                    // If item doesn't exist, add it
                    currentList + BasketItem(product = product, quantity = 1)
                }
            }
            // Trigger recommendation update whenever basket changes
            updateRecommendations()
        }
    }

    fun removeProductFromBasket(productId: String) {
        viewModelScope.launch {
            _basketItems.update { currentList ->
                currentList.filterNot { it.product.id == productId }
            }
            // Trigger recommendation update whenever basket changes
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
            // Optionally trigger recommendation update based on favorites change
            // updateRecommendations()
        }
    }

    fun trackProductClick(productId: String) {
        viewModelScope.launch {
            _clickedProductIds.update { currentClicks ->
                // Keep a limited history or just add unique clicks
                (currentClicks + productId).toList().takeLast(20).toSet()  // Example: keep last 20 unique clicks
            }
            // Optionally trigger recommendation update on clicks
            // updateRecommendations()
        }
    }

    fun searchProducts(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _filteredProducts.value = _allProducts.value
            } else {
                _filteredProducts.value = _allProducts.value.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            DataSource.getCategoryById(it.categoryId)?.name?.contains(query, ignoreCase = true) == true
                    // Add more search criteria if needed (e.g., ingredients)
                }
            }
        }
    }

    // --- AI Recommendation Placeholder ---
    private fun updateRecommendations() {
        viewModelScope.launch {
            // **** THIS IS WHERE YOUR AI MODEL LOGIC WOULD GO ****
            // Input: basketItems.value, favoriteProducts.value, clickedProductIds.value, pastPurchases.value
            // Output: A list of recommended Product objects

            // --- Mock Recommendation Logic ---
            val basketIds = _basketItems.value.map { it.product.id }.toSet()
            val potentialRecs = _allProducts.value.filterNot { product ->
                basketIds.contains(product.id) // Don't recommend items already in basket
            }

            // Simple mock: recommend the first 2 items not in the basket
            val mockRecs = potentialRecs.take(2)
            // --- End Mock Logic ---

            _recommendations.value = mockRecs
        }
    }

    fun getProductById(productId: String): Product? {
        return _allProducts.value.find { it.id == productId }
    }

    fun isFavorite(productId: String): Boolean {
        return _favoriteProducts.value.contains(productId)
    }

    fun isInBasket(productId: String): Boolean {
        return _basketItems.value.any { it.product.id == productId }
    }
}