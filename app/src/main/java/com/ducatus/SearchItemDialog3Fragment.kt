package com.ducatus

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.ducatus.databinding.FragmentSearchItemDialogBinding
import com.ducatus.viewmodel.SearchViewModel3

class SearchItemDialog3Fragment : DialogFragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentSearchItemDialogBinding
    private val searchViewModel3: SearchViewModel3 by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentSearchItemDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tfSearchName.editText?.doOnTextChanged { text, _, _, _ ->
            if (text == null || text.isEmpty()) disableButton()
            else enableButton()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSearch.setOnClickListener {
            val name = binding.tfSearchName.editText?.text.toString().trim { it <= ' ' }
            if (!TextUtils.isEmpty(name)) {
                searchViewModel3.searchName(name)
            }
            dismiss()
        }
    }

    private fun enableButton() {
        binding.btnSearch.isEnabled = true
        binding.btnSearch.setTextColor(ContextCompat.getColor(activity, R.color.green_primary))
    }

    private fun disableButton() {
        binding.btnSearch.isEnabled = false
        binding.btnSearch.setTextColor(ContextCompat.getColor(activity, R.color.gray))
    }
}