package com.ducatus

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.ducatus.interfaces.TipsIntf
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.tipsactivity_main.*

class TipsMainActivity : AppCompatActivity() {
    private lateinit var tabLayout3: TabLayout
    private lateinit var viewPager3: ViewPager2
    private lateinit var tipsIntf: TipsIntf

    companion object {
        lateinit var tipsIntf: TipsIntf

        fun start(context: Context, t: TipsIntf) {
            tipsIntf = t
            val intent = Intent(context, TipsMainActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left)
        setContentView(R.layout.tipsactivity_main)
        tipsIntf = TipsMainActivity.tipsIntf
        tabLayout3 = findViewById(R.id.tabLayoutTips)
        viewPager3 = findViewById(R.id.viewPagerTips)
        viewPager3.adapter = PagerAdapter3(this)
        initListeners()
        TabLayoutMediator(tabLayout3, viewPager3) { tab, index ->
            tab.text = when (index) {
                0 -> {
                    "ARTICLES"
                }
                1 -> {
                    "VIDEOS"
                }
                else -> {
                    throw Resources.NotFoundException("Position Not Found")
                }
            }
        }.attach()
    }

    private fun initListeners() {
        tipsToolbar.setOnClickListener(View.OnClickListener {
            tipsIntf.onToolBarClickListener()
            finish()
        })
    }
}