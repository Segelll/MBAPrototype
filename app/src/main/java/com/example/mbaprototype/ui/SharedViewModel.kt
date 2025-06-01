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
    // Added for the category selector
    data class CategorySelectorItem(val categories: List<Category>, val selectedCategoryId: String?) : ProductListItem
}


class SharedViewModel : ViewModel() {

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    private val _allCategories = MutableStateFlow<List<Category>>(emptyList())
    val allCategories: StateFlow<List<Category>> = _allCategories.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null) // This will now represent selected category ID or search term
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()

    private val _selectedCategoryIdFromTab = MutableStateFlow<String?>(null)
    val selectedCategoryIdFromTab: StateFlow<String?> = _selectedCategoryIdFromTab.asStateFlow()


    val categorizedProductList: StateFlow<List<ProductListItem>> = _searchQuery.map { queryOrCategoryId ->
        val productsToDisplay: List<Product>
        val categoriesToConsider: List<Category>
        val isDetailCategoryView = !queryOrCategoryId.isNullOrBlank() // True if a category is selected or search is active

        val currentProducts = _allProducts.value
        val currentCategories = _allCategories.value

        if (isDetailCategoryView) {
            // Try to find if queryOrCategoryId is a category ID first
            val categoryById = currentCategories.find { it.id == queryOrCategoryId }
            if (categoryById != null) {
                // It's a category ID, filter by it
                productsToDisplay = currentProducts.filter { product -> product.categoryId == queryOrCategoryId }
                categoriesToConsider = listOf(categoryById)
            } else {
                // It's a search query
                productsToDisplay = currentProducts.filter { product ->
                    product.name.contains(queryOrCategoryId!!, ignoreCase = true) ||
                            currentCategories.find { it.id == product.categoryId }?.name?.contains(queryOrCategoryId, ignoreCase = true) == true
                }
                val distinctCategoryIds = productsToDisplay.map { it.categoryId }.distinct()
                categoriesToConsider = currentCategories.filter { distinctCategoryIds.contains(it.id) }
            }
        } else {
            productsToDisplay = currentProducts
            categoriesToConsider = currentCategories
        }

        val productListItems = categoriesToConsider.flatMap { category ->
            val productsInCategory = productsToDisplay.filter { it.categoryId == category.id }
            if (productsInCategory.isNotEmpty()) {
                val header = ProductListItem.CategoryHeader(category, isDetailCategoryView || productsInCategory.size <= 4) // Show all if detailed view or <=4
                val itemsToShow = if (isDetailCategoryView) {
                    productsInCategory.map { ProductListItem.ProductItem(it) }
                } else {
                    // Display up to 4 items when not in detail view (main product screen)
                    productsInCategory.take(4).map { ProductListItem.ProductItem(it) }
                }
                listOf(header) + itemsToShow
            } else {
                emptyList()
            }
        }
        // Prepend category selector if not in detail view (i.e., on the main products screen)
        if (!isDetailCategoryView && currentCategories.isNotEmpty()) {
            listOf(ProductListItem.CategorySelectorItem(currentCategories, _selectedCategoryIdFromTab.value)) + productListItems
        } else {
            productListItems
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )


    private val _basketItems = MutableStateFlow<List<BasketItem>>(emptyList())
    val basketItems: StateFlow<List<BasketItem>> = _basketItems.asStateFlow()

    val basketTotalCost: StateFlow<Double> = _basketItems.map {
        0.0 // Price is not used
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

    // Recommendations for Product Detail Page
    private val _productDetailRecommendations = MutableStateFlow<List<Product>>(emptyList())
    val productDetailRecommendations: StateFlow<List<Product>> = _productDetailRecommendations.asStateFlow()

    // Recommendations for Basket Page (remains the same)
    private val _basketRecommendations = MutableStateFlow<List<Product>>(emptyList())
    val basketRecommendations: StateFlow<List<Product>> = _basketRecommendations.asStateFlow()


    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _allProducts.value = DataSource.products
            _allCategories.value = DataSource.categories
            _pastPurchases.value = DataSource.pastPurchases
            _searchQuery.value = null // Initialize with no filter/search
            updateBasketRecommendations()
        }
    }

    fun getBasketItemQuantity(productId: String): Int {
        return _basketItems.value.find { it.product.id == productId }?.quantity ?: 0
    }


    fun addProductToBasket(product: Product, quantityToAdd: Int = 1) {
        viewModelScope.launch {
            _basketItems.update { currentList ->
                val existingItemIndex = currentList.indexOfFirst { it.product.id == product.id }
                if (existingItemIndex != -1) {
                    val updatedItem = currentList[existingItemIndex].copy(
                        quantity = currentList[existingItemIndex].quantity + quantityToAdd
                    )
                    currentList.toMutableList().apply { set(existingItemIndex, updatedItem) }.toList()
                } else {
                    currentList + BasketItem(product = product, quantity = quantityToAdd)
                }
            }
            updateBasketRecommendations()
            updateProductDetailRecommendations(product.categoryId, product.id) // Update detail page recs
        }
    }

    fun decreaseBasketQuantity(productId: String, quantityToDecrease: Int = 1) {
        viewModelScope.launch {
            _basketItems.update { currentList ->
                val existingItemIndex = currentList.indexOfFirst { it.product.id == productId }
                if (existingItemIndex != -1) {
                    val currentItem = currentList[existingItemIndex]
                    if (currentItem.quantity > quantityToDecrease) {
                        val updatedItem = currentItem.copy(quantity = currentItem.quantity - quantityToDecrease)
                        currentList.toMutableList().apply { set(existingItemIndex, updatedItem) }.toList()
                    } else {
                        // If quantity becomes 0 or less, remove the item
                        currentList.filterNot { it.product.id == productId }
                    }
                } else {
                    currentList // Item not found, return current list
                }
            }
            updateBasketRecommendations()
        }
    }
    fun updateBasketItemQuantity(productId: String, newQuantity: Int) {
        viewModelScope.launch {
            _basketItems.update { currentList ->
                val existingItemIndex = currentList.indexOfFirst { it.product.id == productId }
                if (existingItemIndex != -1) {
                    if (newQuantity > 0) {
                        val updatedItem = currentList[existingItemIndex].copy(quantity = newQuantity)
                        currentList.toMutableList().apply { set(existingItemIndex, updatedItem) }
                    } else {
                        // If new quantity is 0 or less, remove the item
                        currentList.filterNot { it.product.id == productId }
                    }
                } else {
                    currentList // Item not found
                }
            }
            updateBasketRecommendations()
        }
    }


    fun removeProductFromBasket(productId: String) {
        viewModelScope.launch {
            _basketItems.update { currentList ->
                currentList.filterNot { it.product.id == productId }
            }
            updateBasketRecommendations()
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
            val clickedProduct = _allProducts.value.find { it.id == productId }
            clickedProduct?.let {
                updateProductDetailRecommendations(it.categoryId, it.id)
            }
            _clickedProductIds.update { currentClicks ->
                (currentClicks + productId).toList().takeLast(20).toSet()
            }
        }
    }

    // Used by search view and category header clicks (from main product list)
    fun searchProductsOrFilterByCategory(queryOrCategoryName: String?) {
        // Check if the query matches a category name exactly first
        val category = _allCategories.value.find { it.name.equals(queryOrCategoryName, ignoreCase = true) }
        if (category != null) {
            _searchQuery.value = category.id // Filter by category ID
            _selectedCategoryIdFromTab.value = category.id // Also update tab selection if it's a direct category filter
        } else {
            _searchQuery.value = queryOrCategoryName // Treat as a general search query
            if (queryOrCategoryName.isNullOrBlank()){ // If search is cleared, also clear tab selection
                _selectedCategoryIdFromTab.value = null
            }
        }
    }

    // Used by category tabs
    fun filterByCategoryFromTab(categoryId: String?) {
        _selectedCategoryIdFromTab.value = categoryId
        _searchQuery.value = categoryId // Setting searchQuery to categoryId will trigger the filter
        // If categoryId is null (e.g. "All" tab), clear the filter
        if (categoryId == null) {
            clearSearchOrFilter()
        }
    }


    fun clearSearchOrFilter() {
        _searchQuery.value = null
        _selectedCategoryIdFromTab.value = null
    }


    private fun updateBasketRecommendations() {
        viewModelScope.launch {
            val basketProductIds = _basketItems.value.map { it.product.id }.toSet()
            if (_allProducts.value.isEmpty()) {
                _basketRecommendations.value = emptyList()
                return@launch
            }
            val potentialRecs = _allProducts.value.filterNot { product ->
                basketProductIds.contains(product.id) || _favoriteProducts.value.contains(product.id)
            }
            _basketRecommendations.value = potentialRecs.shuffled().take(5) // Show 5 for basket
        }
    }

    // For Product Detail Page recommendations
    fun updateProductDetailRecommendations(categoryId: String?, currentProductId: String?) {
        viewModelScope.launch {
            if (_allProducts.value.isEmpty() || categoryId == null || currentProductId == null) {
                _productDetailRecommendations.value = emptyList()
                return@launch
            }
            val basketProductIds = _basketItems.value.map { it.product.id }.toSet()
            // Recommend other products from the same category, excluding the current one and those in basket/favorites
            val potentialRecs = _allProducts.value.filter { product ->
                product.categoryId == categoryId &&
                        product.id != currentProductId &&
                        !basketProductIds.contains(product.id) &&
                        !_favoriteProducts.value.contains(product.id)
            }
            _productDetailRecommendations.value = potentialRecs.shuffled().take(3) // Show 3 for product detail
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
                items = ArrayList(currentBasket) // Ensure it's a mutable list for safety, though BasketItem is data class
            )
            _pastPurchases.update { currentHistory ->
                listOf(newPurchase) + currentHistory // Prepend to keep newest first
            }
            _basketItems.value = emptyList() // Clear the basket
            updateBasketRecommendations() // Update recommendations as basket is now empty
        }
        return true
    }

    fun getProductById(productId: String): Product? {
        if (_allProducts.value.isEmpty()) {
            Log.w("SharedViewModel", "getProductById called before products were loaded.")
            // Consider loading data if not available, or ensure DataSource.init() is called earlier
            // For now, relying on DataSource being initialized.
            return DataSource.getProductById(productId)
        }
        return _allProducts.value.find { it.id == productId }
    }
    fun getCategoryById(categoryId: String): Category? {
        return _allCategories.value.find { it.id == categoryId } ?: DataSource.getCategoryById(categoryId)
    }

    fun getPurchaseById(id: String): PurchaseHistory? {
        // Check current state flow first
        val purchaseFromState = _pastPurchases.value.find { it.purchaseId == id }
        if (purchaseFromState != null) return purchaseFromState

        // Fallback to DataSource if not found, though ideally StateFlow should be the source of truth after init
        if (DataSource.pastPurchases.any { it.purchaseId == id }) {
            Log.w("SharedViewModel", "getPurchaseById called for an ID in DataSource but not yet in StateFlow.")
            return DataSource.pastPurchases.find { it.purchaseId == id }
        }
        return null
    }

    fun isFavorite(productId: String): Boolean {
        return _favoriteProducts.value.contains(productId)
    }

    fun isInBasket(productId: String): Boolean {
        return _basketItems.value.any { it.product.id == productId }
    }
}