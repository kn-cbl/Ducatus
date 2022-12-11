package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ducatus.adapter.HomeViewPagerAdapter
import com.ducatus.databinding.FragmentHomeBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentHomeBinding
    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        toolbar = activity.findViewById(R.id.tbHome)
        toolbar.inflateMenu(R.menu.notifications_menu)

        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = HomeViewPagerAdapter(childFragmentManager, lifecycle)
        binding.vpHome.adapter = adapter

        val transactionTabs = listOf(
            resources.getString(R.string.overview),
            resources.getString(R.string.budgets_and_goals)
        )

        TabLayoutMediator(binding.tlHome, binding.vpHome) { tab, position ->
            tab.text = transactionTabs[position]
        }.attach()

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.notifications -> {
                    startActivity(Intent(activity, NotificationsActivity::class.java))
                    activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }
                else -> false
            }
        }
    }
}