package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ducatus.adapter.SubscriptionsViewPagerAdapter
import com.ducatus.databinding.FragmentSubscriptionsViewPagerBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayoutMediator

class SubscriptionsViewPagerFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentSubscriptionsViewPagerBinding
    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        toolbar = activity.findViewById(R.id.tbHome)
        toolbar.inflateMenu(R.menu.search_menu)

        binding = FragmentSubscriptionsViewPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SubscriptionsViewPagerAdapter(childFragmentManager, lifecycle)
        binding.vpSubscriptions.adapter = adapter

        val transactionTabs = listOf(
            activity.getString(R.string.one_time),
            activity.getString(R.string.recurring)
        )

        TabLayoutMediator(binding.tlSubscriptions, binding.vpSubscriptions) { tab, position ->
            tab.text = transactionTabs[position]
        }.attach()
    }
}