package com.ducatus

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.ducatus.databinding.FragmentPlannedPaymentRecurrenceDialogBinding

class PlannedPaymentRecurrenceDialogFragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentPlannedPaymentRecurrenceDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentPlannedPaymentRecurrenceDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setMonthDropdown()
        setYearDropdown()

        binding.rbRepeatMonthly.setOnClickListener {
            binding.btnRecurrenceOK.setBackgroundColor(ContextCompat.getColor(activity, R.color.green_primary))
            binding.btnRecurrenceOK.isEnabled = true
            binding.rbRepeatYearly.isChecked = false
            binding.llRecurrenceMonth.visibility = View.VISIBLE
            binding.llRecurrenceYear.visibility = View.GONE
        }

        binding.rbRepeatYearly.setOnClickListener {
            binding.btnRecurrenceOK.setBackgroundColor(ContextCompat.getColor(activity, R.color.green_primary))
            binding.btnRecurrenceOK.isEnabled = true
            binding.rbRepeatMonthly.isChecked = false
            binding.llRecurrenceYear.visibility = View.VISIBLE
            binding.llRecurrenceMonth.visibility = View.GONE
        }


    }

    private fun setMonthDropdown() {
        val months = mutableListOf<Int>()
        for (i in 1 until 12) {
            months.add(i)
        }

        val adapter = ArrayAdapter(activity, R.layout.list_item, months)
        binding.actvRecurrenceMonth.setAdapter(adapter)
        binding.actvRecurrenceMonth.setText(months.first().toString())
    }

    private fun setYearDropdown() {
        val years = mutableListOf<Int>()
        for (i in 1 until 5) {
            years.add(i)
        }

        val adapter = ArrayAdapter(activity, R.layout.list_item, years)
        binding.actvRecurrenceYear.setAdapter(adapter)
        binding.actvRecurrenceYear.setText(years.first().toString())
    }
}