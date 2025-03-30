package com.example.mbaprototype

import android.os.Bundle
import androidx.activity.enableEdgeToEdge // Optional: For edge-to-edge display
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.mbaprototype.databinding.ActivityMainBinding // Import ViewBinding
import com.example.mbaprototype.ui.basket.BasketFragment
import com.example.mbaprototype.ui.history.HistoryFavoritesFragment
import com.example.mbaprototype.ui.products.ProductsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Keep track of fragments to avoid recreating them unnecessarily
    private val productsFragment = ProductsFragment()
    private val historyFavoritesFragment = HistoryFavoritesFragment()
    private val basketFragment = BasketFragment()
    private var activeFragment: Fragment = productsFragment // Start with products

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Optional: Makes the app draw behind status/navigation bars
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Adjust padding for system bars if using edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // Only top padding for root needed usually
            // Let BottomNav handle its own bottom padding
            insets
        }

        setupBottomNavigation()

        // Initialize fragments (add them but show only the active one)
        if (savedInstanceState == null) { // Only add fragments the first time
            supportFragmentManager.beginTransaction()
                .add(R.id.nav_host_fragment_container, basketFragment, "3").hide(basketFragment)
                .add(R.id.nav_host_fragment_container, historyFavoritesFragment, "2").hide(historyFavoritesFragment)
                .add(R.id.nav_host_fragment_container, productsFragment, "1").commit() // Add and show product fragment initially
            activeFragment = productsFragment
        } else {
            // Find existing fragments on configuration change (e.g., rotation)
            val fragment1 = supportFragmentManager.findFragmentByTag("1") ?: productsFragment
            val fragment2 = supportFragmentManager.findFragmentByTag("2") ?: historyFavoritesFragment
            val fragment3 = supportFragmentManager.findFragmentByTag("3") ?: basketFragment
            // Determine which fragment was active - requires saving state or checking selected nav item
            // For simplicity, we'll just default to products or rely on the nav item selection below
            activeFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) ?: productsFragment
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_products -> {
                    switchFragment(productsFragment)
                    true
                }
                R.id.navigation_history_favorites -> {
                    switchFragment(historyFavoritesFragment)
                    true
                }
                R.id.navigation_basket -> {
                    switchFragment(basketFragment)
                    true
                }
                else -> false
            }
        }
        // Optional: Ensure the correct item is selected if activity is recreated
        // binding.bottomNavigationView.selectedItemId = R.id.navigation_products // Or determine based on activeFragment
    }

    private fun switchFragment(fragment: Fragment) {
        if (fragment != activeFragment) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(fragment)
                .commit()
            activeFragment = fragment
        }
    }
}