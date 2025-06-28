package com.example.mbaprototype.ui.products

import android.content.Intent
import android.graphics.drawable.ColorDrawable
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
import com.example.mbaprototype.ui.ProductListItem
import com.example.mbaprototype.ui.SharedViewModel
import com.example.mbaprototype.utils.CategoryColorUtil
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
            populateUI(product)
            setupRecommendationsRecyclerView()
            setupButtonClickListeners(product)
            // DEĞİŞİKLİK: observeViewModelState fonksiyonu artık kategori ID'si almıyor.
            observeViewModelState(product.id)
            // DEĞİŞİKLİK: Başlangıçtaki öneri çağrısı, sadece ürün ID'si alacak şekilde güncellendi.
            sharedViewModel.updateProductDetailRecommendations(product.id)
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
        binding.textDetailProductNameMain.text = product.name

        val category = sharedViewModel.allCategories.value.find { it.id == product.categoryId }
        binding.textDetailCategory.text = category?.name ?: getString(R.string.unknown_category)

        val (backgroundColor, textColor) = CategoryColorUtil.getColorsForCategory(product.categoryId)
        binding.imageDetailProduct.setImageDrawable(ColorDrawable(backgroundColor))

        val words = product.name.split(" ")
        var textToDisplayOnImage = ""
        if (words.isNotEmpty()) {
            val firstWord = words[0]
            if (firstWord.isNotBlank() && firstWord.all { it.isDigit() }) {
                textToDisplayOnImage = if (words.size > 1) {
                    val twoWords = "${words[0]}\n${words[1]}"
                    if (twoWords.length <= 20) twoWords else "${words[0]} ${words[1]}".take(20)
                } else {
                    firstWord.take(12)
                }
            } else {
                textToDisplayOnImage = firstWord.take(12)
            }
        }
        binding.textDetailProductVisualText.text = textToDisplayOnImage.uppercase()
        binding.textDetailProductVisualText.setTextColor(textColor)

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
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                finish()
            },
            onAddToBasketClick = { product ->
                sharedViewModel.addProductToBasket(product)
                Snackbar.make(binding.root, "${product.name} ${getString(R.string.added_updated_in_basket)}", Snackbar.LENGTH_SHORT).show()
            },
            onCategoryHeaderClick = { },
            onCategoryChipSelected = { },
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
            sharedViewModel.addProductToBasket(product)
        }
        binding.buttonQuantityDecrease.setOnClickListener {
            sharedViewModel.decreaseBasketQuantity(product.id)
        }
        binding.buttonQuantityRemove.setOnClickListener {
            sharedViewModel.removeProductFromBasket(product.id)
            Toast.makeText(this, R.string.product_removed_from_basket, Toast.LENGTH_SHORT).show()
        }
        binding.buttonDetailToggleFavorite.setOnClickListener {
            currentProduct?.let { prod ->
                sharedViewModel.toggleFavorite(prod.id)
            }
        }
    }

    // DEĞİŞİKLİK: Fonksiyon imzası, artık kategori ID'si almayacak şekilde güncellendi.
    private fun observeViewModelState(productId: String) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Sepet miktarını gözlemle
                launch {
                    sharedViewModel.basketItems
                        .map { list -> list.find { it.product.id == productId }?.quantity ?: 0 }
                        .distinctUntilChanged()
                        .collect { quantity ->
                            updateQuantityUI(quantity)
                        }
                }
                // Favori durumunu gözlemle
                launch {
                    sharedViewModel.favoriteProducts
                        .map { favorites -> favorites.contains(productId) }
                        .distinctUntilChanged()
                        .collect { isFavorite ->
                            updateFavoriteButtonState(isFavorite)
                        }
                }
                // Önerileri gözlemle
                launch {
                    sharedViewModel.productDetailRecommendations.collect { recommendedProducts ->
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
            binding.buttonQuantityDecrease.isEnabled = true
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
        if (::recommendationsAdapter.isInitialized) {
            binding.recyclerViewDetailRecommendations.adapter = null
        }
        super.onDestroy()
    }
}