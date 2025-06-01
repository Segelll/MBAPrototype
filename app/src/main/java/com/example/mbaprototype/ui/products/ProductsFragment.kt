package com.example.mbaprototype.ui.products

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible // Added import for isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mbaprototype.MBAPrototypeApplication
import com.example.mbaprototype.R
import com.example.mbaprototype.databinding.FragmentProductsBinding
import com.example.mbaprototype.ui.ProductListItem
import com.example.mbaprototype.ui.SharedViewModel
import com.google.android.material.appbar.MaterialToolbar
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
        setupSearchViewAndToolbar()
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
            },
            onCategoryHeaderClick = { category ->
                sharedViewModel.searchProductsOrFilterByCategory(category.id)
                binding.searchViewAlternative.setQuery(category.name, false)
                binding.searchViewAlternative.clearFocus()
            },
            onCategoryChipSelected = { categoryId ->
                sharedViewModel.filterByCategoryFromTab(categoryId)
                if (categoryId != null) {
                    val category = sharedViewModel.allCategories.value.find { it.id == categoryId}
                    binding.searchViewAlternative.setQuery(category?.name ?: "", false)
                } else {
                    binding.searchViewAlternative.setQuery("", false)
                }
                binding.searchViewAlternative.clearFocus()
            },
            sharedViewModel = sharedViewModel
        )

        val gridLayoutManager = GridLayoutManager(context, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                // Check adapter's current list to prevent IndexOutOfBoundsException if list is empty or position is invalid
                if (position < 0 || position >= productAdapter.currentList.size) {
                    return 1 // Default span size or handle error appropriately
                }
                return when (productAdapter.getItemViewType(position)) {
                    VIEW_TYPE_CATEGORY_SELECTOR, VIEW_TYPE_HEADER -> 2
                    VIEW_TYPE_PRODUCT -> 1
                    else -> 1
                }
            }
        }

        binding.recyclerViewProducts.apply {
            adapter = productAdapter
            layoutManager = gridLayoutManager
        }
    }

    private fun setupSearchViewAndToolbar() {
        val mainActivityToolbar = activity?.findViewById<MaterialToolbar>(R.id.toolbar)
        mainActivityToolbar?.let {
            (activity as? AppCompatActivity)?.setSupportActionBar(it)
            sharedViewModel.searchQuery.value?.let { queryOrCatId ->
                val category = sharedViewModel.allCategories.value.find { c -> c.id == queryOrCatId }
                if (category != null) {
                    it.title = category.name
                } else {
                    it.title = getString(R.string.search_results_title, queryOrCatId)
                }
                it.setNavigationIcon(R.drawable.ic_arrow_back)
                it.setNavigationOnClickListener {
                    sharedViewModel.clearSearchOrFilter()
                    binding.searchViewAlternative.setQuery("", false)
                    mainActivityToolbar.title = getString(R.string.products)
                    mainActivityToolbar.navigationIcon = null
                }
            } ?: run {
                it.title = getString(R.string.products)
                it.navigationIcon = null
            }
        }

        binding.searchViewAlternative.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                sharedViewModel.searchProductsOrFilterByCategory(query)
                binding.searchViewAlternative.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // If you want instant filtering as user types:
                // sharedViewModel.searchProductsOrFilterByCategory(newText)
                // However, ensure this doesn't conflict with chip selection logic or become too janky.
                // For now, relying on submit or chip selection.
                if (newText.isNullOrEmpty() && !sharedViewModel.searchQuery.value.isNullOrEmpty() && !sharedViewModel.allCategories.value.any { it.id == sharedViewModel.searchQuery.value }) {
                    // If search text is cleared AND current filter is a search query (not a category ID from a chip), then clear the filter.
                    // This prevents clearing a category chip filter just by clearing search text.
                    // sharedViewModel.clearSearchOrFilter() // This might be too aggressive, consider user experience.
                }
                return true
            }
        })

        binding.searchViewAlternative.setOnCloseListener {
            sharedViewModel.clearSearchOrFilter()
            mainActivityToolbar?.title = getString(R.string.products)
            mainActivityToolbar?.navigationIcon = null
            // binding.searchViewAlternative.setQuery("", false) // Already handled by clearSearchOrFilter observer
            false
        }

        binding.searchViewAlternative.isIconified = false
        binding.searchViewAlternative.clearFocus()
    }


    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.categorizedProductList.collect { listItems ->
                        productAdapter.submitList(listItems)
                        // Only show empty message if there are no products AND no category selector (which means no categories at all)
                        val hasActualProducts = listItems.any { it is ProductListItem.ProductItem }
                        val hasCategorySelector = listItems.any { it is ProductListItem.CategorySelectorItem }

                        binding.textEmptyProducts.isVisible = !hasActualProducts && !hasCategorySelector && sharedViewModel.searchQuery.value != null
                    }
                }
                launch {
                    sharedViewModel.searchQuery.collect { queryOrCatId ->
                        val mainActivityToolbar = activity?.findViewById<MaterialToolbar>(R.id.toolbar)
                        mainActivityToolbar?.let {
                            if (!queryOrCatId.isNullOrBlank()) {
                                val category = sharedViewModel.allCategories.value.find { c -> c.id == queryOrCatId }
                                if (category != null) {
                                    it.title = category.name
                                    if (binding.searchViewAlternative.query.toString() != category.name) {
                                        // binding.searchViewAlternative.setQuery(category.name, false) // Sync search bar if filtered by category chip
                                    }
                                } else {
                                    it.title = getString(R.string.search_results_title, queryOrCatId)
                                    if (binding.searchViewAlternative.query.toString() != queryOrCatId) {
                                        // binding.searchViewAlternative.setQuery(queryOrCatId, false) // Sync search bar with query
                                    }
                                }
                                it.setNavigationIcon(R.drawable.ic_arrow_back)
                                it.setNavigationOnClickListener {
                                    sharedViewModel.clearSearchOrFilter()
                                    // binding.searchViewAlternative.setQuery("", false) // Handled by next block
                                }
                            } else {
                                it.title = getString(R.string.products)
                                it.navigationIcon = null
                                it.setNavigationOnClickListener(null)
                                if (binding.searchViewAlternative.query.isNotEmpty()) {
                                    // binding.searchViewAlternative.setQuery("", false) // Clear search bar if filter is cleared
                                }
                            }
                        }
                        // Sync search view with the actual filter/query state
                        val currentSearchOrFilter = sharedViewModel.searchQuery.value
                        if (currentSearchOrFilter.isNullOrBlank()) {
                            if (binding.searchViewAlternative.query.isNotEmpty()) {
                                binding.searchViewAlternative.setQuery("", false)
                            }
                        } else {
                            val categoryIfId = sharedViewModel.allCategories.value.find { c -> c.id == currentSearchOrFilter }
                            val expectedQueryText = categoryIfId?.name ?: currentSearchOrFilter
                            if (binding.searchViewAlternative.query.toString() != expectedQueryText) {
                                binding.searchViewAlternative.setQuery(expectedQueryText, false)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh based on ViewModel's current state
        val currentQuery = sharedViewModel.searchQuery.value
        val categoryBySelectedId = sharedViewModel.allCategories.value.find { it.id == currentQuery }

        if (categoryBySelectedId != null) {
            binding.searchViewAlternative.setQuery(categoryBySelectedId.name, false)
        } else {
            binding.searchViewAlternative.setQuery(currentQuery, false)
        }
        setupSearchViewAndToolbar() // Re-apply toolbar settings, this will use the latest searchQuery
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewProducts.adapter = null
        _binding = null
    }
}