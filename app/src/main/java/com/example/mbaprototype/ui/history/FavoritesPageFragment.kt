package com.example.mbaprototype.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mbaprototype.MBAPrototypeApplication
import com.example.mbaprototype.databinding.PageFavoritesBinding
import com.example.mbaprototype.ui.SharedViewModel
import com.example.mbaprototype.ui.products.ProductDetailActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FavoritesPageFragment : Fragment() {
    private var _binding: PageFavoritesBinding? = null
    private val binding get() = _binding!!
    private lateinit var favoritesAdapter: FavoritesAdapter
    private val sharedViewModel: SharedViewModel by lazy {
        (requireActivity().application as MBAPrototypeApplication).sharedViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PageFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeFavorites()
    }

    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(
            onFavoriteClick = { product ->
                val intent = Intent(activity, ProductDetailActivity::class.java).apply {
                    putExtra(ProductDetailActivity.EXTRA_PRODUCT, product)
                }
                startActivity(intent)
            },
            onRemoveFavoriteClick = { productId ->
                sharedViewModel.toggleFavorite(productId)
                // Consider adding a Snackbar here for user feedback
            }
        )
        binding.recyclerViewFavoritesPage.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = favoritesAdapter
        }
    }

    private fun observeFavorites() {
        viewLifecycleOwner.lifecycleScope.launch {
            sharedViewModel.favoriteProducts.collectLatest { favoriteProductIds ->
                val favoriteProductsList = favoriteProductIds
                    .mapNotNull { sharedViewModel.getProductById(it) }
                favoritesAdapter.submitList(favoriteProductsList)
                binding.textEmptyFavoritesPage.isVisible = favoriteProductsList.isEmpty()
                binding.recyclerViewFavoritesPage.isVisible = favoriteProductsList.isNotEmpty()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewFavoritesPage.adapter = null // Clear adapter
        _binding = null
    }
}