package com.ducatus

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ducatus.databinding.FragmentBudgetDetailBinding
import com.google.android.material.appbar.MaterialToolbar

class BudgetDetailFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentBudgetDetailBinding
    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        toolbar = activity.findViewById(R.id.tbBudgetDetail)
        toolbar.inflateMenu(R.menu.edit_menu)
        binding = FragmentBudgetDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}