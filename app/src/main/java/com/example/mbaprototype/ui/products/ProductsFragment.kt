package com.example.mbaprototype.ui.products

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mbaprototype.MBAPrototypeApplication
import com.example.mbaprototype.R
import com.example.mbaprototype.data.model.Category
import com.example.mbaprototype.databinding.FragmentProductsBinding
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
        setupSearchViewAndToolbar() // Toolbar'ı da burada ayarla
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
                // Kategori başlığına tıklandığında, o kategori adına göre filtrele
                sharedViewModel.searchProductsOrCategory(category.name)
                binding.searchViewAlternative.setQuery(category.name, false) // Arama çubuğunu güncelle ama submit etme
                binding.searchViewAlternative.clearFocus() // Odağı kaldır
            }
        )
        binding.recyclerViewProducts.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupSearchViewAndToolbar() {
        // Activity'nin toolbar'ını al ve ProductsFragment için ayarla
        val mainActivityToolbar = activity?.findViewById<MaterialToolbar>(R.id.toolbar)
        mainActivityToolbar?.let {
            (activity as? AppCompatActivity)?.setSupportActionBar(it)
            // Geri butonu ve başlık yönetimi
            sharedViewModel.searchQuery.value?.let { query ->
                it.title = query // Filtrelenmiş kategori adını başlık yap
                it.setNavigationIcon(R.drawable.ic_arrow_back)
                it.setNavigationOnClickListener {
                    sharedViewModel.clearSearchOrFilter()
                    binding.searchViewAlternative.setQuery("", false) // Arama çubuğunu temizle
                    mainActivityToolbar.title = getString(R.string.products) // Ana başlığa dön
                    mainActivityToolbar.navigationIcon = null // Geri ikonunu kaldır
                }
            } ?: run {
                it.title = getString(R.string.products)
                it.navigationIcon = null
            }
        }


        binding.searchViewAlternative.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                sharedViewModel.searchProductsOrCategory(query)
                binding.searchViewAlternative.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Kullanıcı yazarken anında filtreleme yapmak istiyorsanız:
                if (newText.isNullOrEmpty() && sharedViewModel.searchQuery.value != null) {
                    // Eğer arama çubuğu aktif bir filtre varken temizlenirse, ana listeye dön
                    // sharedViewModel.clearSearchOrFilter() // Bu anlık filtreleme için kafa karıştırıcı olabilir, submit'te kalsın
                } else {
                    // Anlık filtreleme için: sharedViewModel.searchProductsOrCategory(newText)
                    // Şimdilik sadece submit'te filtreleme yapıyoruz
                }
                return true
            }
        })

        binding.searchViewAlternative.setOnCloseListener {
            // Arama çubuğu kapatıldığında (X butonuna basıldığında)
            sharedViewModel.clearSearchOrFilter()
            mainActivityToolbar?.title = getString(R.string.products)
            mainActivityToolbar?.navigationIcon = null
            false // true dönerse varsayılan davranışı (çubuğu boşaltma) engeller
        }

        // Arama çubuğunun en başta açık olması için
        binding.searchViewAlternative.isIconified = false
        binding.searchViewAlternative.clearFocus() // Başlangıçta odaklanmasın
    }


    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.categorizedProductList.collect { listItems ->
                        productAdapter.submitList(listItems) {
                            // Liste güncellendikten sonra en üste kaydır (opsiyonel)
                            if (listItems.isNotEmpty()) {
                                binding.recyclerViewProducts.scrollToPosition(0)
                            }
                        }
                        // RecyclerView'ın görünürlüğünü de ayarlayabilirsiniz
                        // binding.recyclerViewProducts.isVisible = listItems.isNotEmpty()
                        // binding.textEmptyProducts.isVisible = listItems.isEmpty() // Eğer boş durumu metni varsa
                    }
                }
                // search query'yi dinleyerek toolbar'ı güncelle
                launch {
                    sharedViewModel.searchQuery.collect { query ->
                        val mainActivityToolbar = activity?.findViewById<MaterialToolbar>(R.id.toolbar)
                        mainActivityToolbar?.let {
                            if (!query.isNullOrBlank()) {
                                it.title = query // Veya daha açıklayıcı bir başlık: "Kategori: $query"
                                it.setNavigationIcon(R.drawable.ic_arrow_back)
                                it.setNavigationOnClickListener {
                                    sharedViewModel.clearSearchOrFilter()
                                    binding.searchViewAlternative.setQuery("", false)
                                }
                            } else {
                                it.title = getString(R.string.products)
                                it.navigationIcon = null
                                it.setNavigationOnClickListener(null)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Fragment tekrar görünür olduğunda toolbar'ı ve arama çubuğunu doğru duruma getir
        binding.searchViewAlternative.setQuery(sharedViewModel.searchQuery.value, false)
        // Toolbar'ın da güncellenmesi için observeViewModel içindeki searchQuery collect'i tetiklenecektir.
        // Ya da burada manuel olarak setupSearchViewAndToolbar() çağrılabilir.
        setupSearchViewAndToolbar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewProducts.adapter = null // Bellek sızıntılarını önlemek için
        _binding = null
    }
}