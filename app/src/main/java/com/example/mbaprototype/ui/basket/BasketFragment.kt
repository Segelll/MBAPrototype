package com.example.mbaprototype.ui.basket

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mbaprototype.databinding.FragmentBasketBinding // Import ViewBinding
import com.example.mbaprototype.ui.SharedViewModel
import com.example.mbaprototype.ui.products.ProductAdapter // Reuse ProductAdapter for recommendations
import com.example.mbaprototype.ui.products.ProductDetailActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch


class BasketFragment : Fragment() {

    private var _binding: FragmentBasketBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var basketAdapter: BasketAdapter
    private lateinit var recommendationsAdapter: ProductAdapter // Reuse ProductAdapter

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
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        basketAdapter = BasketAdapter(
            onRemoveClick = { productId ->
                sharedViewModel.removeProductFromBasket(productId)
            }
        )
        binding.recyclerViewBasket.apply {
            adapter = basketAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
        }

        // Use ProductAdapter for recommendations list
        recommendationsAdapter = ProductAdapter(
            onProductClick = { product ->
                // Track click?
                sharedViewModel.trackProductClick(product.id)
                // Navigate to detail
                val intent = Intent(activity, ProductDetailActivity::class.java)
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id)
                startActivity(intent)
            },
            onAddToBasketClick = { product ->
                sharedViewModel.addProductToBasket(product)
                Snackbar.make(binding.root, "${product.name} added to basket", Snackbar.LENGTH_SHORT)
                    .setAnchorView(activity?.findViewById(com.example.mbaprototype.R.id.bottom_navigation_view))
                    .show()
            }
        )
        binding.recyclerViewRecommendations.apply {
            adapter = recommendationsAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) // Horizontal layout? Or Vertical? Let's use Vertical for consistency.
            // layoutManager = LinearLayoutManager(context) // Vertical layout
            itemAnimator = null
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe Basket Items
                launch {
                    sharedViewModel.basketItems.collect { items ->
                        basketAdapter.submitList(items)
                        binding.textEmptyBasket.isVisible = items.isEmpty()
                        binding.recyclerViewBasket.isVisible = items.isNotEmpty()
                    }
                }

                // Observe Recommendations
                launch {
                    sharedViewModel.recommendations.collect { recommendedProducts ->
                        recommendationsAdapter.submitList(recommendedProducts)
                        binding.textEmptyRecommendations.isVisible = recommendedProducts.isEmpty()
                        binding.recyclerViewRecommendations.isVisible = recommendedProducts.isNotEmpty()
                        // Show title only if there are recommendations or basket isn't empty?
                        binding.textRecommendationsTitle.isVisible = recommendedProducts.isNotEmpty()
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