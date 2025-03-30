package com.example.mbaprototype.ui.history

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
import com.example.mbaprototype.MBAPrototypeApplication // Added
import com.example.mbaprototype.databinding.FragmentHistoryFavoritesBinding
import com.example.mbaprototype.ui.SharedViewModel
import com.example.mbaprototype.ui.products.ProductDetailActivity
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HistoryFavoritesFragment : Fragment() {

    private var _binding: FragmentHistoryFavoritesBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by lazy {
        (requireActivity().application as MBAPrototypeApplication).sharedViewModel
    }
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
            onRemoveFavoriteClick = { sharedViewModel.toggleFavorite(it) },
            onFavoriteClick = { product ->
                val intent = Intent(activity, ProductDetailActivity::class.java).apply {
                    putExtra(ProductDetailActivity.EXTRA_PRODUCT, product)
                }
                startActivity(intent)
            }
        )
        binding.recyclerViewFavorites.apply {
            adapter = favoritesAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
        }

        historyAdapter = HistoryAdapter(
            onHistoryClick = { history ->
                if (history.purchaseId.isNotBlank()) {
                    val intent = Intent(activity, PurchaseDetailActivity::class.java).apply {
                        putExtra(PurchaseDetailActivity.EXTRA_PURCHASE_ID, history.purchaseId)
                    }
                    startActivity(intent)
                }
            }
        )
        binding.recyclerViewHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.favoriteProducts
                        .map { favoriteIds -> favoriteIds.mapNotNull { sharedViewModel.getProductById(it) } }
                        .collect { favoriteProductList ->
                            favoritesAdapter.submitList(favoriteProductList)
                            binding.textEmptyFavorites.isVisible = favoriteProductList.isEmpty()
                            binding.recyclerViewFavorites.isVisible = favoriteProductList.isNotEmpty()
                        }
                }

                launch {
                    sharedViewModel.pastPurchases.collect { historyList ->
                        historyAdapter.submitList(historyList)
                        binding.textEmptyHistory.isVisible = historyList.isEmpty()
                        binding.recyclerViewHistory.isVisible = historyList.isNotEmpty()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewFavorites.adapter = null
        binding.recyclerViewHistory.adapter = null
        _binding = null
    }
}