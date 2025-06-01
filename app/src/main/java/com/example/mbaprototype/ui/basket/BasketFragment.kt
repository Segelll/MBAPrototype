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
    private lateinit var recommendationsAdapter: ProductAdapter

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
    }

    private fun setupRecyclerViews() {
        basketAdapter = BasketAdapter { productId ->
            sharedViewModel.removeProductFromBasket(productId)
        }
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
            onCategoryHeaderClick = { category ->
                // BasketFragment'taki recommendationsAdapter için kategori başlığı tıklaması
                // genellikle burada anlamlı bir eylem gerektirmez çünkü kategori başlıkları
                // bu bölümde gösterilmiyor olabilir.
                // İsterseniz buraya ProductsFragment'taki gibi bir filtreleme mantığı ekleyebilirsiniz
                // ya da şimdilik boş bırakabilirsiniz.
                // Örneğin: sharedViewModel.searchProductsOrCategory(category.name)
                // activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)?.selectedItemId = R.id.navigation_products
            }
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
                        binding.dividerBasket.isVisible = !isBasketEmpty
                        binding.textBasketItemCount.text = getString(R.string.basket_item_count, items.sumOf { it.quantity })
                    }
                }
                launch {
                    sharedViewModel.recommendations.collect { recommendedProducts ->
                        // Öneriler bölümü genellikle sadece ürünleri listeler, kategori başlıklarını değil.
                        // Bu yüzden ProductListItem.ProductItem olarak mapliyoruz.
                        // Eğer önerilerde de kategori başlıkları olacaksa, SharedViewModel'deki
                        // recommendations StateFlow'unun ProductListItem döndürmesi gerekir.
                        val listItems = recommendedProducts.map { ProductListItem.ProductItem(it) }
                        recommendationsAdapter.submitList(listItems)
                        val hasRecommendations = listItems.isNotEmpty()
                        binding.textEmptyRecommendations.isVisible = !hasRecommendations
                        binding.recyclerViewRecommendations.isVisible = hasRecommendations
                        binding.textRecommendationsTitle.isVisible = hasRecommendations
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