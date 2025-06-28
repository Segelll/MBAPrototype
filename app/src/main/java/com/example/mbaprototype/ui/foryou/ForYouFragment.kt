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
import androidx.recyclerview.widget.LinearLayoutManager
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

    private lateinit var forYouAdapter: ProductAdapter
    private lateinit var favoritesAdapter: ProductAdapter
    private lateinit var seeAllAdapter: ProductAdapter // "See All" sayfası için yeni adapter

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
        observeViewModel()
        setupClickListeners() // Buton dinleyicilerini ayarlayan fonksiyonu çağır
    }

    private fun setupRecyclerViews() {
        // "For You" (Recommendations1) Adapter
        forYouAdapter = ProductAdapter(
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
        binding.recyclerViewRecommendations1.apply {
            adapter = forYouAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        // "Favorites" (Recommendations2) Adapter
        favoritesAdapter = ProductAdapter(
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
        binding.recyclerViewRecommendations2.apply {
            adapter = favoritesAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        // "See All" Adapter
        seeAllAdapter = ProductAdapter(
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
        binding.recyclerViewSeeAllOverlay.apply {
            adapter = seeAllAdapter
            layoutManager = GridLayoutManager(context, 2) // 2 sütunlu grid görünümü
        }
    }

    private fun setupClickListeners() {
        // "See All" butonuna tıklandığında overlay'i göster
        binding.buttonSeeAll1.setOnClickListener {
            binding.toolbarSeeAllOverlay.title = binding.textRecommendationsTitle1.text // Başlığı ayarla
            binding.seeAllOverlay.isVisible = true
        }

        // Overlay'deki geri butonuna tıklandığında overlay'i gizle
        binding.toolbarSeeAllOverlay.setNavigationOnClickListener {
            binding.seeAllOverlay.isVisible = false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // "Önerilenler" listesini dinle
                launch {
                    sharedViewModel.forYouRecommendations.collect { products ->
                        val hasRecommendations = products.isNotEmpty()
                        binding.headerRow1.isVisible = hasRecommendations
                        binding.recyclerViewRecommendations1.isVisible = hasRecommendations

                        val items = products.map { ProductListItem.ProductItem(it) }
                        forYouAdapter.submitList(items)
                        seeAllAdapter.submitList(items) // See All listesini de güncelle
                    }
                }

                // Favori ürünleri dinle
                launch {
                    sharedViewModel.favoriteProducts.collect { favoriteIds ->
                        val favoriteProducts = favoriteIds.mapNotNull { id ->
                            sharedViewModel.getProductById(id)
                        }

                        val hasFavorites = favoriteProducts.isNotEmpty()
                        binding.headerRow2.isVisible = hasFavorites
                        binding.recyclerViewRecommendations2.isVisible = hasFavorites
                        binding.textRecommendationsTitle2.text = getString(R.string.favorites)

                        val items = favoriteProducts.map { product ->
                            ProductListItem.ProductItem(product)
                        }
                        favoritesAdapter.submitList(items)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerViewRecommendations1.adapter = null
        binding.recyclerViewRecommendations2.adapter = null
        binding.recyclerViewSeeAllOverlay.adapter = null // Adapter'ı null yap
        super.onDestroyView()
        _binding = null
    }
}