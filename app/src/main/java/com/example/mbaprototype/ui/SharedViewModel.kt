package com.example.mbaprototype.ui

import android.app.Application // Import Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel // Change ViewModel to AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mbaprototype.data.DataSource
import com.example.mbaprototype.data.model.BasketItem
import com.example.mbaprototype.data.model.Category
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.data.model.PurchaseHistory
import com.example.mbaprototype.utils.InteractionLogger // Import InteractionLogger
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
    data class CategorySelectorItem(val categories: List<Category>, val selectedCategoryId: String?) : ProductListItem
}

// Change to AndroidViewModel to get Application context
class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    private val _allCategories = MutableStateFlow<List<Category>>(emptyList())
    val allCategories: StateFlow<List<Category>> = _allCategories.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()

    private val _selectedCategoryIdFromTab = MutableStateFlow<String?>(null)
    val selectedCategoryIdFromTab: StateFlow<String?> = _selectedCategoryIdFromTab.asStateFlow()


    val categorizedProductList: StateFlow<List<ProductListItem>> = _searchQuery.map { queryOrCategoryId ->
        val productsToDisplay: List<Product>
        val categoriesToConsider: List<Category>
        val isDetailCategoryView = !queryOrCategoryId.isNullOrBlank()

        val currentProducts = _allProducts.value
        val currentCategories = _allCategories.value

        if (isDetailCategoryView) {
            val categoryById = currentCategories.find { it.id == queryOrCategoryId }
            if (categoryById != null) {
                productsToDisplay = currentProducts.filter { product -> product.categoryId == queryOrCategoryId }
                categoriesToConsider = listOf(categoryById)
            } else {
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
                val header = ProductListItem.CategoryHeader(category, isDetailCategoryView || productsInCategory.size <= 4)
                val itemsToShow = if (isDetailCategoryView) {
                    productsInCategory.map { ProductListItem.ProductItem(it) }
                } else {
                    productsInCategory.take(4).map { ProductListItem.ProductItem(it) }
                }
                listOf(header) + itemsToShow
            } else {
                emptyList()
            }
        }
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

    val basketTotalCost: StateFlow<Double> = _basketItems.map { 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)


    private val _favoriteProducts = MutableStateFlow<Set<String>>(emptySet())
    val favoriteProducts: StateFlow<Set<String>> = _favoriteProducts.asStateFlow()

    private val _pastPurchases = MutableStateFlow<List<PurchaseHistory>>(emptyList())
    val pastPurchases: StateFlow<List<PurchaseHistory>> = _pastPurchases.asStateFlow()

    private val _clickedProductIds = MutableStateFlow<Set<String>>(emptySet())
    val clickedProductIds: StateFlow<Set<String>> = _clickedProductIds.asStateFlow()

    private val _productDetailRecommendations = MutableStateFlow<List<Product>>(emptyList())
    val productDetailRecommendations: StateFlow<List<Product>> = _productDetailRecommendations.asStateFlow()

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
            _searchQuery.value = null
            updateBasketRecommendations()
        }
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
            updateProductDetailRecommendations(product.categoryId, product.id)
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
                        currentList.filterNot { it.product.id == productId }
                    }
                } else {
                    currentList
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
                        currentList.filterNot { it.product.id == productId }
                    }
                } else {
                    currentList
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
            val isCurrentlyFavorite = _favoriteProducts.value.contains(productId)
            _favoriteProducts.update { currentFavorites ->
                if (isCurrentlyFavorite) {
                    currentFavorites - productId
                } else {
                    currentFavorites + productId
                }
            }
            // Log interaction only when adding a favorite
            if (!isCurrentlyFavorite) {
                getProductById(productId)?.let { product ->
                    InteractionLogger.logInteraction(product, "favorites")
                }
            }
        }
    }

    fun trackProductClick(productId: String) {
        viewModelScope.launch {
            // Log interaction
            getProductById(productId)?.let { product ->
                InteractionLogger.logInteraction(product, "click")
            }

            // Existing logic for recommendations and clicked IDs
            val clickedProduct = _allProducts.value.find { it.id == productId }
            clickedProduct?.let {
                updateProductDetailRecommendations(it.categoryId, it.id)
            }
            _clickedProductIds.update { currentClicks ->
                (currentClicks + productId).toList().takeLast(20).toSet()
            }
        }
    }

    fun searchProductsOrFilterByCategory(queryOrCategoryName: String?) {
        val category = _allCategories.value.find { it.name.equals(queryOrCategoryName, ignoreCase = true) }

        if (category != null) {
            _searchQuery.value = category.id
            _selectedCategoryIdFromTab.value = category.id
            // InteractionLogger for search has been removed as it's not in the new API spec.
        } else {
            _searchQuery.value = queryOrCategoryName
            if (queryOrCategoryName.isNullOrBlank()){
                _selectedCategoryIdFromTab.value = null
            }
            // InteractionLogger for search has been removed.
        }
    }

    fun filterByCategoryFromTab(categoryId: String?) {
        _selectedCategoryIdFromTab.value = categoryId
        _searchQuery.value = categoryId
        if (categoryId == null) {
            clearSearchOrFilter()
        } else {
            // InteractionLogger for search has been removed.
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
            _basketRecommendations.value = potentialRecs.shuffled().take(5)
        }
    }

    fun updateProductDetailRecommendations(categoryId: String?, currentProductId: String?) {
        viewModelScope.launch {
            if (_allProducts.value.isEmpty() || categoryId == null || currentProductId == null) {
                _productDetailRecommendations.value = emptyList()
                return@launch
            }
            val basketProductIds = _basketItems.value.map { it.product.id }.toSet()
            val potentialRecs = _allProducts.value.filter { product ->
                product.categoryId == categoryId &&
                        product.id != currentProductId &&
                        !basketProductIds.contains(product.id) &&
                        !_favoriteProducts.value.contains(product.id)
            }
            _productDetailRecommendations.value = potentialRecs.shuffled().take(3)
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

            // Log each purchased item with the "bought" interaction type
            currentBasket.forEach { basketItem ->
                InteractionLogger.logInteraction(
                    basketItem.product,
                    "bought"
                )
            }

            _basketItems.value = emptyList()
            updateBasketRecommendations()
        }
        return true
    }

    // Helper methods
    fun getProductById(productId: String): Product? {
        if (_allProducts.value.isEmpty()) {
            Log.w("SharedViewModel", "getProductById called before products were loaded.")
            return DataSource.getProductById(productId)
        }
        return _allProducts.value.find { it.id == productId }
    }
    fun getCategoryById(categoryId: String): Category? {
        return _allCategories.value.find { it.id == categoryId } ?: DataSource.getCategoryById(categoryId)
    }
    fun getPurchaseById(id: String): PurchaseHistory? {
        val purchaseFromState = _pastPurchases.value.find { it.purchaseId == id }
        if (purchaseFromState != null) return purchaseFromState
        if (DataSource.pastPurchases.any { it.purchaseId == id }) {
            Log.w("SharedViewModel", "getPurchaseById called for an ID in DataSource but not yet in StateFlow.")
            return DataSource.pastPurchases.find { it.purchaseId == id }
        }
        return null
    }
    fun isFavorite(productId: String): Boolean = _favoriteProducts.value.contains(productId)
    fun isInBasket(productId: String): Boolean = _basketItems.value.any { it.product.id == productId }
}