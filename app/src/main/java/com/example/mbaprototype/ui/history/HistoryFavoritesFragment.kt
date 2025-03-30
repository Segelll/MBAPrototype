package com.example.mbaprototype.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible // Extension function for visibility
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mbaprototype.databinding.FragmentHistoryFavoritesBinding // Import ViewBinding
import com.example.mbaprototype.ui.SharedViewModel
import com.example.mbaprototype.ui.products.ProductDetailActivity
import kotlinx.coroutines.launch

class HistoryFavoritesFragment : Fragment() {

    private var _binding: FragmentHistoryFavoritesBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var historyAdapter: HistoryAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        favoritesAdapter = FavoritesAdapter(
            onRemoveFavoriteClick = { productId ->
                sharedViewModel.toggleFavorite(productId) // ViewModel handles removal
            },
            onFavoriteClick = { product ->
                // Track click? Maybe not needed here unless AI uses fav views
                // sharedViewModel.trackProductClick(product.id)
                val intent = Intent(activity, ProductDetailActivity::class.java)
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id)
                startActivity(intent)
            }
        )
        binding.recyclerViewFavorites.apply {
            adapter = favoritesAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null // Optional: disable default animations if needed
        }

        historyAdapter = HistoryAdapter() // No interaction needed for history items for now
        binding.recyclerViewHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null // Optional: disable default animations if needed
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe Favorites
                launch {
                    sharedViewModel.favoriteProducts.collect { favoriteIds ->
                        // Get full product objects from IDs
                        val favoriteProductList = favoriteIds.mapNotNull { sharedViewModel.getProductById(it) }
                        favoritesAdapter.submitList(favoriteProductList)
                        // Show/hide empty message
                        binding.textEmptyFavorites.isVisible = favoriteProductList.isEmpty()
                        binding.recyclerViewFavorites.isVisible = favoriteProductList.isNotEmpty()
                    }
                }

                // Observe History
                launch {
                    sharedViewModel.pastPurchases.collect { historyList ->
                        historyAdapter.submitList(historyList)
                        // Show/hide empty message
                        binding.textEmptyHistory.isVisible = historyList.isEmpty()
                        binding.recyclerViewHistory.isVisible = historyList.isNotEmpty()
                    }
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewFavorites.adapter = null // Clear adapters
        binding.recyclerViewHistory.adapter = null
        _binding = null
    }
}