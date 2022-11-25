package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ducatus.adapter.LoansViewPagerAdapter
import com.ducatus.databinding.FragmentLoansBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayoutMediator

class LoansFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentLoansBinding
    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        toolbar = activity.findViewById(R.id.tbHome)
        toolbar.inflateMenu(R.menu.search_menu)

        binding = FragmentLoansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = LoansViewPagerAdapter(childFragmentManager, lifecycle)
        binding.vpLoans.adapter = adapter

        val transactionTabs = listOf(
            activity.getString(R.string.active),
            activity.getString(R.string.fully_paid)
        )

        TabLayoutMediator(binding.tlLoans, binding.vpLoans) { tab, position ->
            tab.text = transactionTabs[position]
        }.attach()
    }
}