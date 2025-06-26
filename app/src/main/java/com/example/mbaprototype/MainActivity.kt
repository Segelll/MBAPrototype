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
import com.example.mbaprototype.ui.foryou.ForYouFragment
import com.example.mbaprototype.ui.history.HistoryFavoritesFragment
import com.example.mbaprototype.ui.products.ProductsFragment
import com.google.android.material.appbar.AppBarLayout

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Lazily initialize all fragments, including the new ForYouFragment
    private val forYouFragment by lazy { ForYouFragment() }
    private val productsFragment by lazy { ProductsFragment() }
    private val historyFavoritesFragment by lazy { HistoryFavoritesFragment() }
    private val basketFragment by lazy { BasketFragment() }

    // Your original default active fragment is restored
    private var activeFragment: Fragment = productsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // This listener for window insets remains unchanged
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

        // Handle fragment setup on initial creation, preserving your original logic
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.nav_host_fragment_container, basketFragment, "4").hide(basketFragment)
                .add(R.id.nav_host_fragment_container, historyFavoritesFragment, "3").hide(historyFavoritesFragment)
                .add(R.id.nav_host_fragment_container, forYouFragment, "2").hide(forYouFragment) // Add the new fragment, but hide it
                .add(R.id.nav_host_fragment_container, productsFragment, "1") // Show ProductsFragment by default
                .commitNow()
            activeFragment = productsFragment
            updateToolbarTitle(R.string.products) // Set the initial toolbar title to "Products"
        } else {
            // Restore active fragment state after configuration change
            activeFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) ?: productsFragment
            updateToolbarForActiveFragment()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // Add a case for the new "For You" navigation item
                R.id.navigation_for_you -> {
                    switchFragment(forYouFragment, R.string.for_you)
                    true
                }
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
        // This logic correctly sets the selected item based on the active fragment state
        binding.bottomNavigationView.selectedItemId = when (activeFragment) {
            is ForYouFragment -> R.id.navigation_for_you
            is HistoryFavoritesFragment -> R.id.navigation_history_favorites
            is BasketFragment -> R.id.navigation_basket
            else -> R.id.navigation_products // Default to "Products"
        }
    }

    // This function for switching fragments remains unchanged
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

    // This function for updating the toolbar title remains unchanged
    private fun updateToolbarTitle(titleResId: Int) {
        binding.toolbar.title = getString(titleResId)
    }

    // Update this function to handle the title for the new fragment
    private fun updateToolbarForActiveFragment() {
        val titleResId = when (activeFragment) {
            is ForYouFragment -> R.string.for_you
            is HistoryFavoritesFragment -> R.string.history_favorites
            is BasketFragment -> R.string.basket
            is ProductsFragment -> R.string.products
            else -> R.string.app_name
        }
        updateToolbarTitle(titleResId)
    }
}
