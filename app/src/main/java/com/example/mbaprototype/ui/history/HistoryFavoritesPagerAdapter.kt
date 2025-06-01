package com.example.mbaprototype.ui.history

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

private const val NUM_TABS = 2

class HistoryFavoritesPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return NUM_TABS
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HistoryPageFragment()
            1 -> FavoritesPageFragment()
            else -> throw IllegalStateException("Invalid position $position for ViewPager. Expected 0 or 1.")
        }
    }
}