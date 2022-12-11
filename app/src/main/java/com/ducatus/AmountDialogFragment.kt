package com.ducatus

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.ducatus.databinding.FragmentAmountDialogBinding
import com.ducatus.viewmodel.AmountViewModel

class AmountDialogFragment : DialogFragment() {
    private lateinit var binding: FragmentAmountDialogBinding
    private val amountViewModel: AmountViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAmountDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAmountPresetClickListener()

        binding.btnAmountCancel.setOnClickListener {
            dismiss()
        }

        binding.btnAmountConfirm.setOnClickListener {
            val amount = binding.tfAmount.editText?.text.toString().trim { it <= ' '}
            amountViewModel.setAmount(amount)
            dismiss()
        }
    }

    private fun setAmountPresetClickListener() {
        val accountTag = arguments?.getString("account")
        val budgetTag = arguments?.getString("budget")
        var amountList = listOf(
            100, 300, 500,
            1000, 1500, 2000,
            3000, 4000, 5000,
        )

        if (accountTag != null) {
            amountList = listOf(
                1000, 2000, 5000,
                10000, 20000, 30000,
                50000, 75000, 100000
            )
        }

        if (budgetTag != null) {
            amountList = listOf(
                500, 1000, 1500,
                2000, 3000, 4000,
                5000, 7500, 10000
            )
        }

        val gridLayout = binding.glAmountDialog.glAmountPreset
        for (i in 0 until gridLayout.childCount) {
            val gridItem = gridLayout.getChildAt(i) as TextView
            gridItem.text = String.format("%,.0f", amountList[i].toDouble())
            gridItem.tag = amountList[i].toString()

            gridLayout.getChildAt(i).setOnClickListener { item ->
                val amount = item.tag.toString()
                binding.tfAmount.editText?.setText(amount)
            }
        }
    }
}