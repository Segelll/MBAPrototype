package com.example.mbaprototype.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mbaprototype.data.DataSource
import com.example.mbaprototype.data.model.BasketItem
import com.example.mbaprototype.data.model.Category
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.data.model.PurchaseHistory
import com.example.mbaprototype.data.network.AddToBasketRequest
import com.example.mbaprototype.data.network.RetrofitClient
import com.example.mbaprototype.utils.InteractionLogger
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

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.instance

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

    val basketTotalCost: StateFlow<Double> = _basketItems.map { items -> items.sumOf { (it.product.price ?: 0.0) * it.quantity } }
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

    private val _forYouRecommendations = MutableStateFlow<List<Product>>(emptyList())
    val forYouRecommendations: StateFlow<List<Product>> = _forYouRecommendations.asStateFlow()

    private val _forYouLoading = MutableStateFlow(false)
    val forYouLoading: StateFlow<Boolean> = _forYouLoading.asStateFlow()


    init {
        loadInitialData()
        loadBasket()
        loadFavorites()
        updateBasketRecommendations()
    }

    private fun loadInitialData() {
        _allProducts.value = DataSource.products
        _allCategories.value = DataSource.categories
        _pastPurchases.value = DataSource.pastPurchases
        _searchQuery.value = null
    }

    fun loadFavorites() {
        viewModelScope.launch {
            try {
                val response = apiService.getFavoriteItems()
                if (response.isSuccessful) {
                    val favoriteItems = response.body() ?: emptyList()
                    _favoriteProducts.value = favoriteItems.map { it.productNo.toString() }.toSet()
                } else {
                    Log.e("SharedViewModel", "Favoriler yüklenemedi: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Favoriler yüklenirken istisna oluştu", e)
            }
        }
    }

    fun updateForYouRecommendations() {
        viewModelScope.launch {
            _forYouLoading.value = true
            try {
                val response = apiService.getCollaborativeRecommendations("user1", 70)
                if (response.isSuccessful) {
                    val recommendationIds = response.body()?.recommendations ?: emptyList()
                    val allProds = _allProducts.value
                    if (allProds.isEmpty()) {
                        _forYouRecommendations.value = emptyList()
                        return@launch
                    }
                    val recommendedProducts = recommendationIds.mapNotNull { id ->
                        allProds.find { product -> product.id == id.toString() }
                    }
                    _forYouRecommendations.value = recommendedProducts
                } else {
                    _forYouRecommendations.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Failed to fetch 'For You' recommendations", e)
                _forYouRecommendations.value = emptyList()
            } finally {
                _forYouLoading.value = false
            }
        }
    }

    fun loadBasket() {
        viewModelScope.launch {
            try {
                val response = apiService.getBasketItems()
                if (response.isSuccessful) {
                    val remoteBasketProducts = response.body() ?: emptyList()
                    val allProds = _allProducts.value
                    if (allProds.isEmpty()) return@launch

                    val productCounts = mutableMapOf<Int, Int>()
                    for (basketProduct in remoteBasketProducts) {
                        val currentCount = productCounts.getOrDefault(basketProduct.product_no, 0)
                        productCounts[basketProduct.product_no] = currentCount + 1
                    }

                    val newBasketItems = mutableListOf<BasketItem>()
                    for ((productId, quantity) in productCounts) {
                        val foundProduct = allProds.find { product -> product.id == productId.toString() }
                        if (foundProduct != null) {
                            newBasketItems.add(BasketItem(product = foundProduct, quantity = quantity))
                        }
                    }
                    _basketItems.value = newBasketItems
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Failed to load basket", e)
            }
        }
    }


    fun addProductToBasket(product: Product, quantityToAdd: Int = 1) {
        viewModelScope.launch {
            try {
                val response = apiService.addToBasket(AddToBasketRequest(product.id.toInt()))
                if (response.isSuccessful) {
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
                    updateForYouRecommendations()
                    updateProductDetailRecommendations(product.id)
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Failed to add product to basket", e)
            }
        }
    }

    fun decreaseBasketQuantity(productId: String, quantityToDecrease: Int = 1) {
        viewModelScope.launch {
            val currentItem = _basketItems.value.find { it.product.id == productId } ?: return@launch

            if (currentItem.quantity > quantityToDecrease) {
                _basketItems.update { currentList ->
                    val existingItemIndex = currentList.indexOfFirst { it.product.id == productId }
                    val updatedItem = currentItem.copy(quantity = currentItem.quantity - quantityToDecrease)
                    currentList.toMutableList().apply { set(existingItemIndex, updatedItem) }.toList()
                }
            } else {
                try {
                    val response = apiService.deleteFromBasket(productId.toInt())
                    if (response.isSuccessful) {
                        _basketItems.update { currentList ->
                            currentList.filterNot { it.product.id == productId }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SharedViewModel", "Failed to decrease/remove basket item", e)
                }
            }
            updateBasketRecommendations()
            updateForYouRecommendations()
        }
    }

    fun removeProductFromBasket(productId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteFromBasket(productId.toInt())
                if (response.isSuccessful) {
                    _basketItems.update { currentList ->
                        currentList.filterNot { it.product.id == productId }
                    }
                    updateBasketRecommendations()
                    updateForYouRecommendations()
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Failed to remove product from basket", e)
            }
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

            // --- UPDATED LOGIC ---
            // Log all purchased products in a single bulk API call
            val purchasedProducts = currentBasket.map { it.product }
            InteractionLogger.logBulkBoughtInteraction(purchasedProducts)
            // --- END OF UPDATED LOGIC ---

            try {
                val userId = "user1"
                val response = apiService.deleteAllFromBasket(userId)
                if (response.isSuccessful) {
                    Log.d("SharedViewModel", "Remote basket cleared. Re-fetching to confirm.")
                    loadBasket() // This will clear local _basketItems upon successful API response
                    updateBasketRecommendations()
                    updateForYouRecommendations()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("SharedViewModel", "Failed to clear remote basket. Code: ${response.code()}, Error: $errorBody")
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Exception during clearing remote basket", e)
            }
        }
        return true
    }

    private fun updateBasketRecommendations() {
        viewModelScope.launch {
            try {
                val response = apiService.getBasketSimilarityRecommendations("user1", 35)
                if (response.isSuccessful) {
                    val recommendationIds = response.body()?.recommendations ?: emptyList()
                    val allProds = _allProducts.value
                    if (allProds.isEmpty()) {
                        _basketRecommendations.value = emptyList()
                        return@launch
                    }
                    val recommendedProducts = recommendationIds.mapNotNull { id ->
                        allProds.find { product -> product.id == id.toString() }
                    }
                    _basketRecommendations.value = recommendedProducts
                } else {
                    _basketRecommendations.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Failed to fetch basket recommendations", e)
                _basketRecommendations.value = emptyList()
            }
        }
    }

    /**
     * Bir ürünün favori durumunu değiştirir ve ardından Sizin İçin önerilerini günceller.
     */
    fun toggleFavorite(productId: String) {
        viewModelScope.launch {
            val isCurrentlyFavorite = _favoriteProducts.value.contains(productId)

            if (isCurrentlyFavorite) {
                try {
                    val response = apiService.deleteFavorite(productId.toInt())
                    if (response.isSuccessful) {
                        _favoriteProducts.update { currentFavorites ->
                            currentFavorites - productId
                        }
                    } else {
                        Log.e("SharedViewModel", "Favori silinemedi $productId: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("SharedViewModel", "Favori silinirken istisna oluştu $productId", e)
                }
            } else {
                getProductById(productId)?.let { product ->
                    InteractionLogger.logInteraction(product, "favorites")
                    _favoriteProducts.update { currentFavorites ->
                        currentFavorites + productId
                    }
                }
            }

            // GÜNCELLEME: Favori durumu değiştiğinde önerileri güncelle.
            updateForYouRecommendations()
        }
    }

    /**
     * Ürün tıklamasını takip eder ve ardından Sizin İçin önerilerini günceller.
     */
    fun trackProductClick(productId: String) {
        viewModelScope.launch {
            getProductById(productId)?.let { product ->
                InteractionLogger.logInteraction(product, "click")
            }
            updateProductDetailRecommendations(productId)

            _clickedProductIds.update { currentClicks ->
                (currentClicks + productId).toList().takeLast(20).toSet()
            }

            // GÜNCELLEME: Ürün tıklandığında önerileri güncelle.
            updateForYouRecommendations()
        }
    }

    fun searchProductsOrFilterByCategory(queryOrCategoryName: String?) {
        val category = _allCategories.value.find { it.name.equals(queryOrCategoryName, ignoreCase = true) }
        if (category != null) {
            _searchQuery.value = category.id
            _selectedCategoryIdFromTab.value = category.id
        } else {
            _searchQuery.value = queryOrCategoryName
            if (queryOrCategoryName.isNullOrBlank()){
                _selectedCategoryIdFromTab.value = null
            }
        }
    }

    fun filterByCategoryFromTab(categoryId: String?) {
        _selectedCategoryIdFromTab.value = categoryId
        _searchQuery.value = categoryId
        if (categoryId == null) {
            clearSearchOrFilter()
        }
    }

    fun clearSearchOrFilter() {
        _searchQuery.value = null
        _selectedCategoryIdFromTab.value = null
    }

    fun updateProductDetailRecommendations(productId: String?) {
        if (productId == null) {
            _productDetailRecommendations.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val response = apiService.getContentBasedRecommendations(productId.toInt(), 17)
                if (response.isSuccessful) {
                    val recommendationIds = response.body()?.recommendations ?: emptyList()
                    val allProds = _allProducts.value
                    if (allProds.isEmpty()) {
                        _productDetailRecommendations.value = emptyList()
                        return@launch
                    }
                    val recommendedProducts = recommendationIds.mapNotNull { id ->
                        allProds.find { product -> product.id == id.toString() }
                    }
                    _productDetailRecommendations.value = recommendedProducts
                } else {
                    _productDetailRecommendations.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Failed to fetch content-based recommendations", e)
                _productDetailRecommendations.value = emptyList()
            }
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