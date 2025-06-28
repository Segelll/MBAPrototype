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

    // Adapter'ların isimleri layout'a uygun olacak şekilde güncellendi
    private lateinit var forYouAdapter: ProductAdapter
    private lateinit var favoritesAdapter: ProductAdapter

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
    }

    private fun setupRecyclerViews() {
        // "For You" (Recommendations1) RecyclerView Adapter Kurulumu
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
        // DÜZELTME: Layout dosyasındaki doğru ID kullanıldı: recycler_view_recommendations1
        binding.recyclerViewRecommendations1.apply {
            adapter = forYouAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        // "Favorites" (Recommendations2) RecyclerView Adapter Kurulumu
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
        // DÜZELTME: Layout dosyasındaki doğru ID kullanıldı: recycler_view_recommendations2
        binding.recyclerViewRecommendations2.apply {
            adapter = favoritesAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // "For You" önerilerini dinle ve ilgili UI bileşenlerini güncelle
                launch {
                    sharedViewModel.forYouRecommendations.collect { products ->
                        val hasRecommendations = products.isNotEmpty()
                        // DÜZELTME: Layout dosyasındaki doğru ID'ler kullanıldı.
                        binding.headerRow1.isVisible = hasRecommendations
                        binding.recyclerViewRecommendations1.isVisible = hasRecommendations

                        val items = products.map { ProductListItem.ProductItem(it) }
                        forYouAdapter.submitList(items)
                    }
                }

                // Favori ürünleri dinle ve ilgili UI bileşenlerini güncelle
                launch {
                    sharedViewModel.favoriteProducts.collect { favoriteIds ->
                        val favoriteProducts = favoriteIds.mapNotNull { id ->
                            sharedViewModel.getProductById(id)
                        }

                        val hasFavorites = favoriteProducts.isNotEmpty()
                        // DÜZELTME: Layout dosyasındaki doğru ID'ler kullanıldı.
                        binding.headerRow2.isVisible = hasFavorites
                        binding.recyclerViewRecommendations2.isVisible = hasFavorites
                        // Başlık metni de layout'a uygun olarak güncellendi.
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
        // DÜZELTME: Doğru ID'ler kullanılarak adapter'lar null yapıldı.
        binding.recyclerViewRecommendations1.adapter = null
        binding.recyclerViewRecommendations2.adapter = null
        super.onDestroyView()
        _binding = null
    }
}