package com.example.mbaprototype.ui.products

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView // Correct import
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mbaprototype.databinding.FragmentProductsBinding // Import ViewBinding
import com.example.mbaprototype.ui.SharedViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!! // Property delegate to access binding safely

    // Use activityViewModels() to get the ViewModel scoped to the Activity
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onProductClick = { product ->
                // Track click
                sharedViewModel.trackProductClick(product.id)
                // Navigate to Product Detail Screen
                val intent = Intent(activity, ProductDetailActivity::class.java)
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id)
                startActivity(intent)
            },
            onAddToBasketClick = { product ->
                sharedViewModel.addProductToBasket(product)
                // Show a confirmation message
                Snackbar.make(binding.root, "${product.name} added to basket", Snackbar.LENGTH_SHORT)
                    .setAnchorView(activity?.findViewById(com.example.mbaprototype.R.id.bottom_navigation_view)) // Anchor to bottom nav
                    .show()
            }
        )
        binding.recyclerViewProducts.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Trigger search when user submits (e.g., presses enter)
                sharedViewModel.searchProducts(query.orEmpty())
                binding.searchView.clearFocus() // Hide keyboard
                return true // Query handled
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Trigger search as user types
                sharedViewModel.searchProducts(newText.orEmpty())
                return true // Query handled
            }
        })

        // Optional: Handle closing the search view
        binding.searchView.setOnCloseListener {
            sharedViewModel.searchProducts("") // Clear search when closed
            false // Allow default behavior (clearing text)
        }
    }


    private fun observeViewModel() {
        // Use viewLifecycleOwner.lifecycleScope for safety
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle ensures collection stops when view is paused/destroyed
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.filteredProducts.collect { products ->
                    productAdapter.submitList(products)
                }
            }
        }
        // Observe basket/favorites if needed to update UI elements in the list (e.g., disable add button)
        // This requires more complex adapter logic or exposing combined state from ViewModel.
        // For now, the add button here always just adds.
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Important to prevent memory leaks
    }
}