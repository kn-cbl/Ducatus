package com.ducatus

import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import com.ducatus.PagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class LoanMainActivity : AppCompatActivity() {
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.loanactivity_main)

        tabLayout = findViewById(R.id.tabLayoutLoans)
        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = PagerAdapter(this)

        TabLayoutMediator(tabLayout,viewPager) { tab,index ->
            tab.text = when(index) {
                0 -> { "ACTIVE" }
                1 -> { "FULLY PAID" }
                else -> { throw Resources.NotFoundException("Position Not Found")}
            }
        }.attach()
    }

}