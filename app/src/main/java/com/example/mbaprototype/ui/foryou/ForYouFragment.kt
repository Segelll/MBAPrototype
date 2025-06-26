package com.example.mbaprototype.ui.foryou

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mbaprototype.MBAPrototypeApplication
import com.example.mbaprototype.R
import com.example.mbaprototype.data.DataSource
import com.example.mbaprototype.data.model.Product
import com.example.mbaprototype.databinding.FragmentForYouBinding
import com.example.mbaprototype.ui.ProductListItem
import com.example.mbaprototype.ui.SharedViewModel
import com.example.mbaprototype.ui.products.ProductAdapter
import com.example.mbaprototype.ui.products.ProductDetailActivity
import com.google.android.material.snackbar.Snackbar

class ForYouFragment : Fragment() {

    private var _binding: FragmentForYouBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by lazy {
        (requireActivity().application as MBAPrototypeApplication).sharedViewModel
    }

    private lateinit var recommendationsAdapter1: ProductAdapter
    private lateinit var recommendationsAdapter2: ProductAdapter
    private lateinit var seeAllAdapter: ProductAdapter

    private var recommendationsList1: List<Product> = emptyList()
    private var recommendationsList2: List<Product> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForYouBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        populateRecommendationsFromDataSource()
        setupClickListeners()
        handleOnBackPressed()
    }

    private fun setupClickListeners() {
        // "See all" buttons on the main page
        binding.buttonSeeAll1.setOnClickListener {
            showOverlayWithData(recommendationsList1, binding.textRecommendationsTitle1.text.toString())
        }
        binding.buttonSeeAll2.setOnClickListener {
            showOverlayWithData(recommendationsList2, binding.textRecommendationsTitle2.text.toString())
        }

        // Back button on the overlay toolbar
        binding.toolbarSeeAllOverlay.setNavigationOnClickListener {
            hideOverlay()
        }
    }

    private fun setupRecyclerViews() {
        // Adapter for horizontal recommendations row 1
        recommendationsAdapter1 = createHorizontalAdapter()
        binding.recyclerViewRecommendations1.apply {
            adapter = recommendationsAdapter1
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        // Adapter for horizontal recommendations row 2
        recommendationsAdapter2 = createHorizontalAdapter()
        binding.recyclerViewRecommendations2.apply {
            adapter = recommendationsAdapter2
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        // Adapter for the "See All" overlay grid
        seeAllAdapter = createGridAdapter()
        binding.recyclerViewSeeAllOverlay.apply {
            adapter = seeAllAdapter
            layoutManager = GridLayoutManager(context, 2)
        }
    }

    private fun populateRecommendationsFromDataSource() {
        val allProducts = DataSource.products
        if (allProducts.isEmpty()) {
            binding.headerRow1.isVisible = false
            binding.recyclerViewRecommendations1.isVisible = false
            binding.headerRow2.isVisible = false
            binding.recyclerViewRecommendations2.isVisible = false
            return
        }

        val shuffledProducts = allProducts.shuffled()

        recommendationsList1 = shuffledProducts.take(10)
        val listItems1 = recommendationsList1.map { ProductListItem.ProductItem(it) }
        recommendationsAdapter1.submitList(listItems1)
        binding.headerRow1.isVisible = true
        binding.recyclerViewRecommendations1.isVisible = true

        recommendationsList2 = shuffledProducts.drop(10).take(10)
        if (recommendationsList2.isNotEmpty()) {
            val listItems2 = recommendationsList2.map { ProductListItem.ProductItem(it) }
            recommendationsAdapter2.submitList(listItems2)
            binding.headerRow2.isVisible = true
            binding.recyclerViewRecommendations2.isVisible = true
        } else {
            binding.headerRow2.isVisible = false
            binding.recyclerViewRecommendations2.isVisible = false
        }
    }

    private fun showOverlayWithData(products: List<Product>, title: String) {
        binding.toolbarSeeAllOverlay.title = title
        val items = products.map { ProductListItem.ProductItem(it) }
        seeAllAdapter.submitList(items)
        binding.seeAllOverlay.isVisible = true
    }

    private fun hideOverlay() {
        binding.seeAllOverlay.isVisible = false
    }

    private fun handleOnBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.seeAllOverlay.isVisible) {
                    hideOverlay()
                } else {
                    // If the overlay is not visible, allow the default back press action
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    isEnabled = true // Must be re-enabled
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    // Helper to create a ProductAdapter for horizontal recommendation lists
    private fun createHorizontalAdapter(): ProductAdapter {
        return ProductAdapter(
            onProductClick = { product ->
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
    }

    // Helper to create a ProductAdapter for the vertical grid in the overlay
    private fun createGridAdapter(): ProductAdapter {
        return ProductAdapter(
            onProductClick = { product ->
                val intent = Intent(activity, ProductDetailActivity::class.java).apply {
                    putExtra(ProductDetailActivity.EXTRA_PRODUCT, product)
                }
                startActivity(intent)
            },
            onAddToBasketClick = { product ->
                sharedViewModel.addProductToBasket(product)
                Snackbar.make(binding.root, "${product.name} ${getString(R.string.added_updated_in_basket)}", Snackbar.LENGTH_SHORT).show()
            },
            onCategoryHeaderClick = {},
            onCategoryChipSelected = {},
            sharedViewModel = sharedViewModel
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewRecommendations1.adapter = null
        binding.recyclerViewRecommendations2.adapter = null
        binding.recyclerViewSeeAllOverlay.adapter = null
        _binding = null
    }
}