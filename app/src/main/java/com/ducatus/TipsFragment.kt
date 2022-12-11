package com.ducatus

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ducatus.adapter.TipsViewPagerAdapter
import com.ducatus.databinding.FragmentTipsBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayoutMediator

class TipsFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentTipsBinding
    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        toolbar = activity.findViewById(R.id.tbHome)
        toolbar.inflateMenu(R.menu.search_menu)

        binding = FragmentTipsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TipsViewPagerAdapter(childFragmentManager, lifecycle)
        binding.vpTips.adapter = adapter

        val transactionTabs = listOf(
            activity.resources.getString(R.string.articles),
            activity.resources.getString(R.string.videos)
        )

        TabLayoutMediator(binding.tlTips, binding.vpTips) { tab, position ->
            tab.text = transactionTabs[position]
        }.attach()
    }
}