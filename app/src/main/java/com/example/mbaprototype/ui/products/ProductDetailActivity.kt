package com.example.mbaprototype.ui.products

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// import androidx.core.content.ContextCompat // Artık selector kullanıldığı için gerekmeyebilir
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mbaprototype.MBAPrototypeApplication
import com.example.mbaprototype.R
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.databinding.ActivityProductDetailBinding
import com.example.mbaprototype.ui.SharedViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private val sharedViewModel: SharedViewModel by lazy {
        (application as MBAPrototypeApplication).sharedViewModel
    }
    private var currentProduct: Product? = null

    companion object {
        const val EXTRA_PRODUCT = "extra_product"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
            setupButtonClickListeners(product)
            observeViewModelState(product.id)
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
        binding.collapsingToolbarLayout.title = product.name
        binding.textDetailProductName.text = product.name
        // Fiyat gösterilmiyor

        // Kategori adını SharedViewModel'den alarak göster
        val category = sharedViewModel.allCategories.value.find { it.id == product.categoryId }
        binding.textDetailCategory.text = getString(R.string.category_prefix, category?.name ?: getString(R.string.unknown_category))

        // İçerik bilgilerini göster
        if (product.ingredients.isNullOrEmpty()) {
            binding.textDetailIngredients.isVisible = false
            binding.textNoIngredients.isVisible = true
        } else {
            binding.textDetailIngredients.text = product.ingredients.joinToString(", ")
            binding.textDetailIngredients.isVisible = true
            binding.textNoIngredients.isVisible = false
        }
    }

    private fun setupButtonClickListeners(product: Product) {
        binding.buttonDetailAddInitial.setOnClickListener {
            sharedViewModel.addProductToBasket(product)
        }
        binding.buttonQuantityIncrease.setOnClickListener {
            sharedViewModel.addProductToBasket(product) // Zaten sepetteyse miktarını artırır
        }
        binding.buttonQuantityDecrease.setOnClickListener {
            sharedViewModel.decreaseBasketQuantity(product.id)
        }
        binding.buttonQuantityRemove.setOnClickListener {
            sharedViewModel.removeProductFromBasket(product.id)
            Toast.makeText(this, R.string.product_removed_from_basket, Toast.LENGTH_SHORT).show()
        }
        binding.buttonDetailToggleFavorite.setOnClickListener {
            // Tıklama anındaki currentProduct'ı kullanmak daha güvenli
            currentProduct?.let { prod ->
                sharedViewModel.toggleFavorite(prod.id)
            }
        }
    }

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
            }
        }
    }

    private fun updateQuantityUI(quantity: Int) {
        if (quantity > 0) {
            binding.textQuantity.text = quantity.toString()
            binding.quantitySelectorGroup.isVisible = true
            binding.buttonDetailAddInitial.isVisible = false
            binding.buttonQuantityDecrease.isEnabled = true // Miktar 1'den fazlaysa azaltma butonu aktif
        } else {
            binding.quantitySelectorGroup.isVisible = false
            binding.buttonDetailAddInitial.isVisible = true
        }
    }

    // Favori butonunun görsel durumunu `isSelected` ile yönet
    private fun updateFavoriteButtonState(isFavorite: Boolean) {
        binding.buttonDetailToggleFavorite.isSelected = isFavorite
        binding.buttonDetailToggleFavorite.contentDescription = if (isFavorite) {
            getString(R.string.remove_from_favorites)
        } else {
            getString(R.string.add_to_favorites)
        }
    }
}