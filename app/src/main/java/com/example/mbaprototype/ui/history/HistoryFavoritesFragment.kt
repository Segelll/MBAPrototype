package com.example.mbaprototype.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mbaprototype.R
import com.example.mbaprototype.databinding.FragmentHistoryFavoritesBinding
import com.google.android.material.tabs.TabLayoutMediator

class HistoryFavoritesFragment : Fragment() {

    private var _binding: FragmentHistoryFavoritesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // This inflates R.layout.fragment_history_favorites
        _binding = FragmentHistoryFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPagerAndTabs()
    }

    private fun setupViewPagerAndTabs() {
        // These expect @+id/view_pager_history_favorites and @+id/tab_layout_history_favorites in your XML
        val viewPager = binding.mainContentPager
        val tabLayout = binding.topTabsSelector

        // Ensure HistoryFavoritesPagerAdapter.kt exists and is in this package or imported correctly
        viewPager.adapter = HistoryFavoritesPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.title_history)
                1 -> getString(R.string.title_favorites)
                else -> null
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}