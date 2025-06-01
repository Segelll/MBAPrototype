package com.example.mbaprototype.ui.products

import android.content.Intent
import android.graphics.drawable.ColorDrawable // Import ColorDrawable
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mbaprototype.MBAPrototypeApplication
import com.example.mbaprototype.R
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.databinding.ActivityProductDetailBinding
import com.example.mbaprototype.ui.ProductListItem // Might be needed if recommendationsAdapter uses it directly.
import com.example.mbaprototype.ui.SharedViewModel
import com.example.mbaprototype.utils.CategoryColorUtil // Import your utility
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private val sharedViewModel: SharedViewModel by lazy {
        (application as MBAPrototypeApplication).sharedViewModel
    }
    private var currentProduct: Product? = null
    private lateinit var recommendationsAdapter: ProductAdapter // Adapter for recommendations

    companion object {
        const val EXTRA_PRODUCT = "extra_product"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.product_details_title) // Generic title


        currentProduct = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PRODUCT, Product::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PRODUCT)
        }

        if (currentProduct == null) {
            showErrorAndFinish()
            return
        }

        currentProduct?.let { product ->
            populateUI(product) // Call this first
            setupRecommendationsRecyclerView() // Then setup RV
            setupButtonClickListeners(product) // Then listeners
            observeViewModelState(product.id, product.categoryId) // Then observe
            sharedViewModel.updateProductDetailRecommendations(product.categoryId, product.id) // Initial call for recommendations
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun showErrorAndFinish() {
        Toast.makeText(this, R.string.error_product_not_found, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun populateUI(product: Product) {
        // Set the main product name in the content area
        binding.textDetailProductNameMain.text = product.name

        val category = sharedViewModel.allCategories.value.find { it.id == product.categoryId }
        binding.textDetailCategory.text = category?.name ?: getString(R.string.unknown_category)

        // --- Logic to fill the top image area (visual placeholder) ---
        val (backgroundColor, textColor) = CategoryColorUtil.getColorsForCategory(product.categoryId)
        binding.imageDetailProduct.setImageDrawable(ColorDrawable(backgroundColor)) // Set background color

        val words = product.name.split(" ")
        var textToDisplayOnImage = ""
        if (words.isNotEmpty()) {
            val firstWord = words[0]
            if (firstWord.isNotBlank() && firstWord.all { it.isDigit() }) { // Check if the first word is a number
                textToDisplayOnImage = if (words.size > 1) {
                    // Using newline for two words, and slightly longer take limit for detail page
                    val twoWords = "${words[0]}\n${words[1]}"
                    if (twoWords.length <= 20) twoWords else "${words[0]} ${words[1]}".take(20)
                } else {
                    firstWord.take(12) // Only one word (a number)
                }
            } else {
                textToDisplayOnImage = firstWord.take(12) // First word is not a number
            }
        }
        binding.textDetailProductVisualText.text = textToDisplayOnImage.uppercase()
        binding.textDetailProductVisualText.setTextColor(textColor)
        // --- End of logic for the visual placeholder ---

        // Ingredients
        if (product.ingredients.isNullOrEmpty()) {
            binding.textDetailIngredients.isVisible = false
            binding.textNoIngredients.isVisible = true
        } else {
            binding.textDetailIngredients.text = product.ingredients.joinToString(", ")
            binding.textDetailIngredients.isVisible = true
            binding.textNoIngredients.isVisible = false
        }
    }

    private fun setupRecommendationsRecyclerView() {
        recommendationsAdapter = ProductAdapter(
            onProductClick = { product ->
                sharedViewModel.trackProductClick(product.id)
                val intent = Intent(this, ProductDetailActivity::class.java).apply {
                    putExtra(ProductDetailActivity.EXTRA_PRODUCT, product)
                    // Clear top and start new task to refresh if it's the same activity, or manage stack
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                finish() // Finish current detail to avoid stacking if navigating to another product detail
            },
            onAddToBasketClick = { product ->
                sharedViewModel.addProductToBasket(product)
                Snackbar.make(binding.root, "${product.name} ${getString(R.string.added_updated_in_basket)}", Snackbar.LENGTH_SHORT).show()
            },
            // These are not strictly needed for a simple horizontal list of recommended products
            // if those recommendations don't have their own headers/chips within the recommendations list itself.
            onCategoryHeaderClick = { /* No category headers in this recommendation list */ },
            onCategoryChipSelected = { /* No category chips in this recommendation list */ },
            sharedViewModel = sharedViewModel
        )
        binding.recyclerViewDetailRecommendations.apply {
            adapter = recommendationsAdapter
            layoutManager = LinearLayoutManager(this@ProductDetailActivity, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupButtonClickListeners(product: Product) {
        binding.buttonDetailAddInitial.setOnClickListener {
            sharedViewModel.addProductToBasket(product)
        }
        binding.buttonQuantityIncrease.setOnClickListener {
            sharedViewModel.addProductToBasket(product) // ViewModel handles incrementing quantity
        }
        binding.buttonQuantityDecrease.setOnClickListener {
            sharedViewModel.decreaseBasketQuantity(product.id)
        }
        binding.buttonQuantityRemove.setOnClickListener {
            sharedViewModel.removeProductFromBasket(product.id)
            Toast.makeText(this, R.string.product_removed_from_basket, Toast.LENGTH_SHORT).show()
        }
        binding.buttonDetailToggleFavorite.setOnClickListener {
            currentProduct?.let { prod -> // Use currentProduct to ensure it's not null
                sharedViewModel.toggleFavorite(prod.id)
            }
        }
    }

    private fun observeViewModelState(productId: String, categoryId: String?) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe basket quantity for the current product
                launch {
                    sharedViewModel.basketItems
                        .map { list -> list.find { it.product.id == productId }?.quantity ?: 0 }
                        .distinctUntilChanged()
                        .collect { quantity ->
                            updateQuantityUI(quantity)
                        }
                }
                // Observe favorite status for the current product
                launch {
                    sharedViewModel.favoriteProducts
                        .map { favorites -> favorites.contains(productId) }
                        .distinctUntilChanged()
                        .collect { isFavorite ->
                            updateFavoriteButtonState(isFavorite)
                        }
                }
                // Observe recommendations
                launch {
                    sharedViewModel.productDetailRecommendations.collect { recommendedProducts ->
                        // The ProductAdapter expects List<ProductListItem>
                        // Map your Product list to ProductListItem.ProductItem
                        val listItems = recommendedProducts.map { ProductListItem.ProductItem(it) }
                        recommendationsAdapter.submitList(listItems)
                        binding.textDetailRecommendationsTitle.isVisible = listItems.isNotEmpty()
                        binding.recyclerViewDetailRecommendations.isVisible = listItems.isNotEmpty()
                        binding.textNoDetailRecommendations.isVisible = listItems.isEmpty()
                    }
                }
            }
        }
    }

    private fun updateQuantityUI(quantity: Int) {
        if (quantity > 0) {
            binding.textQuantity.text = quantity.toString()
            binding.quantitySelectorGroup.isVisible = true
            binding.buttonDetailAddInitial.isVisible = false
            binding.buttonQuantityDecrease.isEnabled = true // Or quantity > 1 depending on desired behavior
        } else {
            binding.quantitySelectorGroup.isVisible = false
            binding.buttonDetailAddInitial.isVisible = true
            binding.buttonQuantityDecrease.isEnabled = false
        }
    }

    private fun updateFavoriteButtonState(isFavorite: Boolean) {
        binding.buttonDetailToggleFavorite.isSelected = isFavorite
        binding.buttonDetailToggleFavorite.contentDescription = if (isFavorite) {
            getString(R.string.remove_from_favorites)
        } else {
            getString(R.string.add_to_favorites)
        }
    }

    override fun onDestroy() {
        // Clear adapter to prevent memory leaks from RecyclerView
        if (::recommendationsAdapter.isInitialized) { // Check if initialized
            binding.recyclerViewDetailRecommendations.adapter = null
        }
        super.onDestroy()
    }
}