package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ducatus.databinding.FragmentPlannedPaymentsBinding
import com.google.android.material.tabs.TabLayoutMediator

class PlannedPaymentsFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentPlannedPaymentsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentPlannedPaymentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PlannedPaymentsViewPagerAdapter(childFragmentManager, lifecycle)
        binding.vpPlannedPayments.adapter = adapter

        val transactionTabs = listOf(
            activity.getString(R.string.one_time),
            activity.getString(R.string.recurring)
        )

        TabLayoutMediator(binding.tlPlannedPayments, binding.vpPlannedPayments) { tab, position ->
            tab.text = transactionTabs[position]
        }.attach()

        binding.fabAddPlannedPayment.setOnClickListener {
            startActivity(Intent(activity, PlannedPaymentAddActivity::class.java))
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}