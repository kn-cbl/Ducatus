package com.ducatus

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.ducatus.databinding.FragmentSubscriptionRecurrenceDialogBinding
import com.ducatus.viewmodel.SubscriptionRecurrenceViewModel

class SubscriptionRecurrenceDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentSubscriptionRecurrenceDialogBinding
    private var recurrence = 0
    private val recurrenceViewModel: SubscriptionRecurrenceViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentSubscriptionRecurrenceDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setMonthDropdown()

        binding.rbRepeatMonthly.setOnClickListener {
            binding.rbRepeatYearly.isChecked = false
            binding.llRecurrenceMonth.visibility = View.VISIBLE
            enableButton()
        }

        binding.rbRepeatYearly.setOnClickListener {
            recurrence = 12

            binding.rbRepeatMonthly.isChecked = false
            binding.llRecurrenceMonth.visibility = View.GONE
            enableButton()
        }

        binding.spRecurrenceMonth.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                recurrence = parent?.getItemAtPosition(position) as Int
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }

        binding.btnRecurrenceCancel.setOnClickListener {
            dismiss()
        }

        binding.btnRecurrenceOK.setOnClickListener {
            if (recurrence > 0) {
                recurrenceViewModel.setRecurrence(recurrence)
                dismiss()
            }
        }
    }

    private fun setMonthDropdown() {
        val months = mutableListOf<Int>()
        for (i in 1 until 12) {
            months.add(i)
        }

        val adapter = ArrayAdapter(activity, R.layout.list_item, months)
        binding.spRecurrenceMonth.adapter = adapter
    }

    private fun enableButton() {
        binding.btnRecurrenceOK.setTextColor(ContextCompat.getColor(activity, R.color.green_primary))
        binding.btnRecurrenceOK.isEnabled = true
    }
}