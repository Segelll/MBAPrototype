package com.example.mbaprototype.ui.foryou

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
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mbaprototype.MBAPrototypeApplication
import com.example.mbaprototype.R
import com.example.mbaprototype.databinding.FragmentForYouBinding
import com.example.mbaprototype.ui.ProductListItem
import com.example.mbaprototype.ui.SharedViewModel
import com.example.mbaprototype.ui.products.ProductAdapter
import com.example.mbaprototype.ui.products.ProductDetailActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ForYouFragment : Fragment() {

    private var _binding: FragmentForYouBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by lazy {
        (requireActivity().application as MBAPrototypeApplication).sharedViewModel
    }

    private lateinit var recommendationsAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForYouBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        sharedViewModel.updateForYouRecommendations()
    }

    private fun setupRecyclerView() {
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
            onCategoryHeaderClick = {},
            onCategoryChipSelected = {},
            sharedViewModel = sharedViewModel
        )
        binding.recyclerViewRecommendations.apply {
            adapter = recommendationsAdapter
            layoutManager = GridLayoutManager(context, 2)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.forYouRecommendations.collect { products ->
                        val items = products.map { ProductListItem.ProductItem(it) }
                        recommendationsAdapter.submitList(items)
                    }
                }

                launch {
                    sharedViewModel.forYouLoading.collect { isLoading ->
                        binding.progressBarForYou.isVisible = isLoading
                        binding.recyclerViewRecommendations.isVisible = !isLoading
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerViewRecommendations.adapter = null
        super.onDestroyView()
        _binding = null
    }
}