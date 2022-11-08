package com.ducatus

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MotionEventCompat
import androidx.viewpager2.widget.ViewPager2
import com.ducatus.interfaces.GoalIntf
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.fragment_goalactivity_main.*

class GoalMainActivity : AppCompatActivity() {
    private lateinit var tabLayout2: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var toolbarListener: GoalIntf
    private var currentIndex = 0

    companion object {
        lateinit var goalIntf: GoalIntf
        fun start(mContext: Context, mGoalIntf: GoalIntf) {
            goalIntf = mGoalIntf
            val intent: Intent = Intent(mContext, GoalMainActivity::class.java)
            mContext.startActivity(intent)
        }

        fun getInterface(): GoalIntf {
            return goalIntf
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_goalactivity_main)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
        initViews()
        initListeners()
    }

    private fun initListeners() {
        goalsToolbar.setOnClickListener { it ->
            finish()
            toolbarListener.OnToolbarClickListener(it)
        }
    }

    private fun initViews() {
        toolbarListener = GoalMainActivity.getInterface()

        tabLayout2 = findViewById(R.id.tabLayoutGoals)
        viewPager2 = findViewById(R.id.viewPagerGoals)
        viewPager2.adapter = PagerAdapter2(this)

        TabLayoutMediator(tabLayout2, viewPager2) { tab, index ->
            tab.text = when (index) {
                0 -> {
                    "ACTIVE"
                }
                1 -> {
                    "PAUSED"
                }
                2 -> {
                    "REACHED"
                }
                else -> {
                    throw Resources.NotFoundException("Position Not Found")
                }
            }
            currentIndex = index
        }.attach()

    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val action = MotionEventCompat.getActionMasked(event)
        when (action) {
            MotionEvent.ACTION_DOWN -> {

            }
        }
        return super.onTouchEvent(event)
    }
}