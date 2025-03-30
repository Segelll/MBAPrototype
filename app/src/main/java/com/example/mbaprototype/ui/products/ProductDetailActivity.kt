package com.example.mbaprototype.ui.products

import android.content.res.ColorStateList // For ColorStateList.valueOf()
import android.graphics.Color            // For Color.GRAY fallback
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mbaprototype.R
import com.example.mbaprototype.data.DataSource
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.databinding.ActivityProductDetailBinding
import com.example.mbaprototype.ui.SharedViewModel
import com.google.android.material.color.MaterialColors // For MaterialColors.getColor()
import kotlinx.coroutines.launch


class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private val sharedViewModel: SharedViewModel by viewModels()
    private var currentProduct: Product? = null

    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable the back button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val productId = intent.getStringExtra(EXTRA_PRODUCT_ID)

        if (productId == null) {
            // Handle error: No product ID provided
            Toast.makeText(this, R.string.error_product_not_found, Toast.LENGTH_LONG).show()
            finish() // Close the activity
            return
        }

        // Get product details (could be from ViewModel or DataSource directly in this simple case)
        // Using ViewModel is better if product data could change or needs fetching
        // Let's load it directly from ViewModel's knowledge of all products for simplicity here
        currentProduct = sharedViewModel.getProductById(productId)


        if (currentProduct == null) {
            // Handle error: Product ID not found in data source
            Toast.makeText(this, R.string.error_product_not_found, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        populateUI(currentProduct!!)
        setupButtonClickListeners(currentProduct!!)
        observeViewModelState(currentProduct!!.id) // Observe basket/fav state for this product
    }

    // Handle the action bar back button press
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed() // Recommended way to handle back press
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun populateUI(product: Product) {
        supportActionBar?.title = product.name // Set Activity title
        binding.textDetailProductName.text = product.name
        binding.textDetailProductPrice.text = getString(R.string.price_format, product.price)

        // Display category
        val category = DataSource.getCategoryById(product.categoryId)
        binding.textDetailCategory.text = getString(R.string.category_prefix, category?.name ?: "N/A")


        // Display ingredients
        if (product.ingredients.isNullOrEmpty()) {
            binding.textDetailIngredients.isVisible = false
            binding.textNoIngredients.isVisible = true
            // Adjust constraints if needed, though visibility="gone" often handles it
            binding.divider2.layoutParams = (binding.divider2.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).apply {
                topToBottom = binding.textNoIngredients.id // Constraint divider below the "no ingredients" text
            }

        } else {
            binding.textDetailIngredients.text = product.ingredients.joinToString(", ")
            binding.textDetailIngredients.isVisible = true
            binding.textNoIngredients.isVisible = false
            binding.divider2.layoutParams = (binding.divider2.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).apply {
                topToBottom = binding.textDetailIngredients.id // Constraint divider below the ingredients text
            }
        }

        // Load image using a library like Coil or Glide in a real app
        // E.g., binding.imageDetailProduct.load(product.imageUrl) { placeholder(R.drawable.placeholder) }
    }

    private fun setupButtonClickListeners(product: Product) {
        binding.buttonDetailAddToBasket.setOnClickListener {
            if (sharedViewModel.isInBasket(product.id)) {
                // Optional: Maybe navigate to basket or just show message
                Toast.makeText(this, "${product.name} is already in the basket", Toast.LENGTH_SHORT).show()
            } else {
                sharedViewModel.addProductToBasket(product)
                // Update button state immediately for responsiveness (will be confirmed by observer)
                updateBasketButtonState(true)
                Toast.makeText(this, "${product.name} added to basket", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonDetailToggleFavorite.setOnClickListener {
            sharedViewModel.toggleFavorite(product.id)
            // Update button state immediately (will be confirmed by observer)
            updateFavoriteButtonState(!sharedViewModel.isFavorite(product.id)) // Toggle immediately for UI feel
        }
    }

    // Observe ViewModel state (basket & favorites) specific to this product
    private fun observeViewModelState(productId: String) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.basketItems.collect { basket ->
                        val isInBasket = basket.any { it.product.id == productId }
                        updateBasketButtonState(isInBasket)
                    }
                }
                launch {
                    sharedViewModel.favoriteProducts.collect { favorites ->
                        val isFavorite = favorites.contains(productId)
                        updateFavoriteButtonState(isFavorite)
                    }
                }
            }
        }
    }


    private fun updateBasketButtonState(isInBasket: Boolean) {
        if (isInBasket) {
            binding.buttonDetailAddToBasket.text = getString(R.string.in_basket)
            binding.buttonDetailAddToBasket.setIconResource(R.drawable.ic_remove_shopping_cart) // Or a checkmark icon?
            binding.buttonDetailAddToBasket.isEnabled = false // Disable adding again
            binding.buttonDetailAddToBasket.alpha = 0.7f // Visually indicate disabled
        } else {
            binding.buttonDetailAddToBasket.text = getString(R.string.add_to_basket)
            binding.buttonDetailAddToBasket.setIconResource(R.drawable.ic_add_shopping_cart)
            binding.buttonDetailAddToBasket.isEnabled = true
            binding.buttonDetailAddToBasket.alpha = 1.0f
        }
    }

    private fun updateFavoriteButtonState(isFavorite: Boolean) {
        if (isFavorite) {
            binding.buttonDetailToggleFavorite.text = getString(R.string.remove_from_favorites)
            binding.buttonDetailToggleFavorite.setIconResource(R.drawable.ic_favorite_filled)
            // Set tint to secondary color when favorited
            val colorSecondary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondary, Color.GRAY) // Resolve theme attribute
            binding.buttonDetailToggleFavorite.iconTint = ColorStateList.valueOf(colorSecondary)

        } else {
            binding.buttonDetailToggleFavorite.text = getString(R.string.add_to_favorites)
            binding.buttonDetailToggleFavorite.setIconResource(R.drawable.ic_favorite_border)
            // Set tint to secondary color even for border (common practice) or set to null to use default outline tint
            val colorSecondary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondary, Color.GRAY)
            binding.buttonDetailToggleFavorite.iconTint = ColorStateList.valueOf(colorSecondary)
            // Alternatively, to use the button's default outline tint:
            // binding.buttonDetailToggleFavorite.iconTint = null
        }
    }
}