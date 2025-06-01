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
    data class CategoryHeader(val category: Category, val isShowingAll: Boolean = false) : ProductListItem
    data class ProductItem(val product: Product) : ProductListItem
}


class SharedViewModel : ViewModel() {

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    private val _allCategories = MutableStateFlow<List<Category>>(emptyList())
    val allCategories: StateFlow<List<Category>> = _allCategories.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()


    val categorizedProductList: StateFlow<List<ProductListItem>> = _searchQuery.map { query ->
        val productsToDisplay: List<Product>
        val categoriesToConsider: List<Category>
        val isDetailCategoryView = !query.isNullOrBlank()

        if (isDetailCategoryView) {
            productsToDisplay = _allProducts.value.filter { product ->
                product.name.contains(query!!, ignoreCase = true) ||
                        _allCategories.value.find { it.id == product.categoryId }?.name?.contains(query, ignoreCase = true) == true
            }
            val distinctCategoryIds = productsToDisplay.map { it.categoryId }.distinct()
            categoriesToConsider = _allCategories.value.filter { distinctCategoryIds.contains(it.id) }

        } else {
            productsToDisplay = _allProducts.value
            categoriesToConsider = _allCategories.value
        }

        categoriesToConsider.flatMap { category ->
            val productsInCategory = productsToDisplay.filter { it.categoryId == category.id }
            if (productsInCategory.isNotEmpty()) {
                val header = ProductListItem.CategoryHeader(category, isDetailCategoryView || productsInCategory.size <= 3)
                val itemsToShow = if (isDetailCategoryView) {
                    productsInCategory.map { ProductListItem.ProductItem(it) }
                } else {
                    productsInCategory.take(3).map { ProductListItem.ProductItem(it) }
                }
                listOf(header) + itemsToShow
            } else {
                emptyList()
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )


    private val _basketItems = MutableStateFlow<List<BasketItem>>(emptyList())
    val basketItems: StateFlow<List<BasketItem>> = _basketItems.asStateFlow()

    val basketTotalCost: StateFlow<Double> = _basketItems.map {
        0.0
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0.0
    )


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
            _searchQuery.value = null
            updateRecommendations()
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
                // Önce Set'i List'e dönüştür, sonra takeLast uygula, sonra tekrar Set'e dönüştür.
                (currentClicks + productId).toList().takeLast(20).toSet()
            }
        }
    }

    fun searchProductsOrCategory(query: String?) {
        _searchQuery.value = query
    }

    fun clearSearchOrFilter() {
        _searchQuery.value = null
    }


    private fun updateRecommendations() {
        viewModelScope.launch {
            val basketProductIds = _basketItems.value.map { it.product.id }.toSet()
            if (_allProducts.value.isEmpty()) {
                _recommendations.value = emptyList()
                return@launch
            }
            val potentialRecs = _allProducts.value.filterNot { product ->
                basketProductIds.contains(product.id) || _favoriteProducts.value.contains(product.id)
            }
            _recommendations.value = potentialRecs.shuffled().take(3)
        }
    }

    fun completePurchase(): Boolean {
        val currentBasket = _basketItems.value
        if (currentBasket.isEmpty()) {
            return false
        }
        viewModelScope.launch {
            val newPurchase = PurchaseHistory(
                purchaseId = UUID.randomUUID().toString(),
                purchaseDate = Date(),
                items = ArrayList(currentBasket)
            )
            _pastPurchases.update { currentHistory ->
                listOf(newPurchase) + currentHistory
            }
            _basketItems.value = emptyList()
            updateRecommendations()
        }
        return true
    }

    fun getProductById(productId: String): Product? {
        if (_allProducts.value.isEmpty()) {
            Log.w("SharedViewModel", "getProductById called before products were loaded.")
            return null
        }
        return _allProducts.value.find { it.id == productId }
    }

    fun getPurchaseById(id: String): PurchaseHistory? {
        if (_pastPurchases.value.isEmpty() && DataSource.pastPurchases.any { it.purchaseId == id }) {
            Log.w("SharedViewModel", "getPurchaseById called for an ID that might be in DataSource but not yet in StateFlow.")
        }
        return _pastPurchases.value.find { it.purchaseId == id }
    }

    fun isFavorite(productId: String): Boolean {
        return _favoriteProducts.value.contains(productId)
    }

    fun isInBasket(productId: String): Boolean {
        return _basketItems.value.any { it.product.id == productId }
    }
}