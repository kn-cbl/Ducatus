package com.ducatus

import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class GoalMainActivity : AppCompatActivity() {
    private lateinit var tabLayout2: TabLayout
    private lateinit var viewPager2: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.goalactivity_main)

        tabLayout2 = findViewById(R.id.tabLayoutGoals)
        viewPager2 = findViewById(R.id.viewPagerGoals)
        viewPager2.adapter = PagerAdapter2(this)

        TabLayoutMediator(tabLayout2,viewPager2) { tab,index ->
            tab.text = when(index) {
                0 -> { "ACTIVE" }
                1 -> { "PAUSED" }
                2 -> { "REACHED" }
                else -> { throw Resources.NotFoundException("Position Not Found")}
            }
        }.attach()
    }
}