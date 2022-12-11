package com.ducatus.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ducatus.TipsArticlesFragment
import com.ducatus.TipsVideosFragment

private const val NUM_TABS = 2

class TipsViewPagerAdapter(
    fm: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount(): Int {
        return NUM_TABS
    }

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return TipsArticlesFragment()
        }
        return TipsVideosFragment()
    }
}