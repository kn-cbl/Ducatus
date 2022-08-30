package com.ducatus

import android.content.res.Resources
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class PagerAdapter3(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> { TipsArticlesFragment() }
            1 -> { TipsVideosFragment() }
            else -> { throw Resources.NotFoundException("Position Not Found")}
        }
    }
}