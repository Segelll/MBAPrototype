package com.example.mbaprototype.ui.basket

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mbaprototype.MBAPrototypeApplication
import com.example.mbaprototype.R
import com.example.mbaprototype.databinding.FragmentBasketBinding
import com.example.mbaprototype.ui.ProductListItem
import com.example.mbaprototype.ui.SharedViewModel
import com.example.mbaprototype.ui.products.ProductAdapter
import com.example.mbaprototype.ui.products.ProductDetailActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class BasketFragment : Fragment() {

    private var _binding: FragmentBasketBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by lazy {
        (requireActivity().application as MBAPrototypeApplication).sharedViewModel
    }
    private lateinit var basketAdapter: BasketAdapter
    private lateinit var recommendationsAdapter: ProductAdapter // Using ProductAdapter for recommendations

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBasketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupPurchaseButton()
        observeViewModel()

        // Sepet ekranı her açıldığında verileri sunucudan yükle
        sharedViewModel.loadBasket()
    }

    private fun setupRecyclerViews() {
        basketAdapter = BasketAdapter(
            onRemoveClick = { productId ->
                sharedViewModel.removeProductFromBasket(productId)
            },
            onIncreaseClick = { productId ->
                val product = sharedViewModel.basketItems.value.find { it.product.id == productId }?.product
                product?.let { sharedViewModel.addProductToBasket(it) }
            },
            onDecreaseClick = { productId ->
                sharedViewModel.decreaseBasketQuantity(productId)
            }
        )
        binding.recyclerViewBasket.apply {
            adapter = basketAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
        }

        recommendationsAdapter = ProductAdapter(
            onProductClick = { product ->
                sharedViewModel.trackProductClick(product.id)
                val intent = Intent(activity, ProductDetailActivity::class.java).apply {
                    putExtra(ProductDetailActivity.EXTRA_PRODUCT, product)
                }
                startActivity(intent)
            },
            onAddToBasketClick = { product ->
                sharedViewModel.addProductToBasket(product)
                Snackbar.make(binding.root, "${product.name} ${getString(R.string.added_updated_in_basket)}", Snackbar.LENGTH_SHORT)
                    .setAnchorView(activity?.findViewById(R.id.bottom_navigation_view))
                    .show()
            },
            onCategoryHeaderClick = { /* Not used for horizontal recommendations */ },
            onCategoryChipSelected = { /* Not used */},
            sharedViewModel = sharedViewModel
        )
        binding.recyclerViewRecommendations.apply {
            adapter = recommendationsAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = null
        }
    }

    private fun setupPurchaseButton() {
        binding.buttonCompletePurchase.setOnClickListener {
            val success = sharedViewModel.completePurchase()
            if (success) {
                Snackbar.make(binding.root, R.string.purchase_successful, Snackbar.LENGTH_LONG)
                    .setAnchorView(activity?.findViewById(R.id.bottom_navigation_view))
                    .show()
            } else {
                Snackbar.make(binding.root, R.string.basket_empty_cannot_purchase, Snackbar.LENGTH_SHORT)
                    .setAnchorView(activity?.findViewById(R.id.bottom_navigation_view))
                    .show()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.basketItems.collect { items ->
                        basketAdapter.submitList(items)
                        val isBasketEmpty = items.isEmpty()
                        binding.textEmptyBasket.isVisible = isBasketEmpty
                        binding.recyclerViewBasket.isVisible = !isBasketEmpty
                        binding.buttonCompletePurchase.isEnabled = !isBasketEmpty
                        binding.purchaseBar.isVisible = !isBasketEmpty
                        binding.dividerBasket.isVisible = !isBasketEmpty && binding.recyclerViewRecommendations.isVisible
                        binding.textBasketItemCount.text = getString(R.string.basket_item_count, items.sumOf { it.quantity })
                    }
                }
                // DÜZELTME: Bu blok kaldırıldı çünkü textTotalPrice ID'si layout'ta yok.
                // launch {
                //    sharedViewModel.basketTotalCost.collect { total ->
                //         binding.textTotalPrice.text = getString(R.string.total_price_formatted, total)
                //    }
                // }
                launch {
                    sharedViewModel.basketRecommendations.collect { recommendedProducts ->
                        val listItems = recommendedProducts.map { ProductListItem.ProductItem(it) }
                        recommendationsAdapter.submitList(listItems)
                        val hasRecommendations = listItems.isNotEmpty()
                        binding.textRecommendationsTitle.isVisible = hasRecommendations
                        binding.recyclerViewRecommendations.isVisible = hasRecommendations
                        binding.textEmptyRecommendations.isVisible = !hasRecommendations
                        binding.dividerBasket.isVisible = !binding.textEmptyBasket.isVisible && hasRecommendations
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewBasket.adapter = null
        binding.recyclerViewRecommendations.adapter = null
        _binding = null
    }
}