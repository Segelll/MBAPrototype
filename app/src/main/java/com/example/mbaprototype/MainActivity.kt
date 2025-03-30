package com.example.mbaprototype

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.mbaprototype.databinding.ActivityMainBinding
import com.example.mbaprototype.ui.basket.BasketFragment
import com.example.mbaprototype.ui.history.HistoryFavoritesFragment
import com.example.mbaprototype.ui.products.ProductsFragment
import com.google.android.material.appbar.AppBarLayout

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val productsFragment by lazy { ProductsFragment() }
    private val historyFavoritesFragment by lazy { HistoryFavoritesFragment() }
    private val basketFragment by lazy { BasketFragment() }
    private var activeFragment: Fragment = productsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContainer) { view, windowInsets ->
            val systemBarsMask = WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars() or
                    WindowInsetsCompat.Type.captionBar()
            val insets = windowInsets.getInsets(systemBarsMask)

            view.findViewById<AppBarLayout>(R.id.app_bar_layout)?.updatePadding(top = insets.top)
            binding.bottomNavigationView.updatePadding(bottom = insets.bottom)

            WindowInsetsCompat.CONSUMED
        }

        setupBottomNavigation()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.nav_host_fragment_container, basketFragment, "3").hide(basketFragment)
                .add(R.id.nav_host_fragment_container, historyFavoritesFragment, "2").hide(historyFavoritesFragment)
                .add(R.id.nav_host_fragment_container, productsFragment, "1")
                .commitNow()
            activeFragment = productsFragment
            updateToolbarTitle(R.string.products)
        } else {
            activeFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) ?: productsFragment
            updateToolbarForActiveFragment()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_products -> {
                    switchFragment(productsFragment, R.string.products)
                    true
                }
                R.id.navigation_history_favorites -> {
                    switchFragment(historyFavoritesFragment, R.string.history_favorites)
                    true
                }
                R.id.navigation_basket -> {
                    switchFragment(basketFragment, R.string.basket)
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigationView.selectedItemId = when (activeFragment) {
            is HistoryFavoritesFragment -> R.id.navigation_history_favorites
            is BasketFragment -> R.id.navigation_basket
            else -> R.id.navigation_products
        }
    }

    private fun switchFragment(fragment: Fragment, titleResId: Int) {
        if (fragment != activeFragment) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(fragment)
                .commit()
            activeFragment = fragment
            updateToolbarTitle(titleResId)
        }
    }

    private fun updateToolbarTitle(titleResId: Int) {
        binding.toolbar.title = getString(titleResId)
    }

    private fun updateToolbarForActiveFragment() {
        val titleResId = when (activeFragment) {
            is HistoryFavoritesFragment -> R.string.history_favorites
            is BasketFragment -> R.string.basket
            else -> R.string.products
        }
        updateToolbarTitle(titleResId)
    }
}