package com.example.mbaprototype.ui.products

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mbaprototype.MBAPrototypeApplication
import com.example.mbaprototype.R
import com.example.mbaprototype.databinding.FragmentProductsBinding
import com.example.mbaprototype.ui.SharedViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ProductsFragment : Fragment() {

    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by lazy {
        (requireActivity().application as MBAPrototypeApplication).sharedViewModel
    }
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
            }
        )
        binding.recyclerViewProducts.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupSearchView() {
        binding.searchViewAlternative.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                sharedViewModel.searchProducts(query.orEmpty())
                binding.searchViewAlternative.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                sharedViewModel.searchProducts(newText.orEmpty())
                return true
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.categorizedProductList.collect { listItems ->
                    productAdapter.submitList(listItems)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewProducts.adapter = null
        _binding = null
    }
}