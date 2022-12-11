package com.ducatus.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ducatus.ChallengesActiveFragment
import com.ducatus.ChallengesNewFragment

private const val NUM_TABS = 2

class ChallengesViewPagerAdapter(
    fm: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount(): Int {
        return NUM_TABS
    }

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return ChallengesActiveFragment()
        }
        return ChallengesNewFragment()
    }
}