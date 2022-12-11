package com.ducatus

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ducatus.adapter.ChallengesViewPagerAdapter
import com.ducatus.databinding.FragmentChallengesBinding
import com.google.android.material.tabs.TabLayoutMediator

class ChallengesFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentChallengesBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentChallengesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val adapter = ChallengesViewPagerAdapter(childFragmentManager, lifecycle)
        binding.vpChallenges.adapter = adapter

        val transactionTabs = listOf(
            resources.getString(R.string.active),
            resources.getString(R.string.new_)
        )

        TabLayoutMediator(binding.tlChallenges, binding.vpChallenges) { tab, position ->
            tab.text = transactionTabs[position]
        }.attach()
    }
}