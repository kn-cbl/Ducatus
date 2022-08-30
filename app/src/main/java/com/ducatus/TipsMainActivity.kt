package com.ducatus

import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TipsMainActivity : AppCompatActivity() {
    private lateinit var tabLayout3: TabLayout
    private lateinit var viewPager3: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tipsactivity_main)

        tabLayout3 = findViewById(R.id.tabLayoutTips)
        viewPager3 = findViewById(R.id.viewPagerTips)
        viewPager3.adapter = PagerAdapter3(this)

        TabLayoutMediator(tabLayout3,viewPager3) { tab,index ->
            tab.text = when(index) {
                0 -> { "ARTICLES" }
                1 -> { "VIDEOS" }
                else -> { throw Resources.NotFoundException("Position Not Found")}
            }
        }.attach()
    }
}