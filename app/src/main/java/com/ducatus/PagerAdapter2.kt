package com.ducatus

import android.content.Context
import android.content.res.Resources
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class PagerAdapter2(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {



    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> { GoalActiveFragment() }
            1 -> { GoalPausedFragment() }
            2 -> { GoalReachedFragment() }
            else -> { throw Resources.NotFoundException("Position Not Found")}
        }
    }


}