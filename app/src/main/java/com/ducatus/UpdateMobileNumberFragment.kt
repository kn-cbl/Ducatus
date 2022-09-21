package com.ducatus

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentUpdateMobileNumberBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class UpdateMobileNumberFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentUpdateMobileNumberBinding
    private var mobileNumberRegex = "^[89][0-9]{9}$"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentUpdateMobileNumberBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inputObserver()

        binding.btnUpdateMobileNumber.setOnClickListener {
            validateMobileNumber()
        }
    }

    private fun inputObserver() {
        binding.tfUpdateMobileNumber.editText?.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 0) binding.tfUpdateMobileNumber.error = getString(R.string.mobile_number_empty)
            else if (!mobileNumberRegex.toRegex().matches(text!!)) binding.tfUpdateMobileNumber.error = getString(R.string.mobile_number_invalid)
            else binding.tfUpdateMobileNumber.error = null
        }
    }

    private fun validateMobileNumber() {
        clearErrors()

        val mobileNumber = binding.tfUpdateMobileNumber.editText?.text.toString().trim {it <= ' '}
        if (mobileNumberRegex.toRegex().matches(mobileNumber)) {
            val action = UpdateMobileNumberFragmentDirections.actionUpdateMobileNumberFragmentToVerifyUpdateMobileNumberFragment(mobileNumber)
            findNavController().navigate(action)
        }
        else {
            if (!mobileNumberRegex.toRegex().matches(mobileNumber)) binding.tfUpdateMobileNumber.error = getString(R.string.mobile_number_invalid)
            if (TextUtils.isEmpty(mobileNumber)) binding.tfUpdateMobileNumber.error = getString(R.string.mobile_number_empty)
        }
    }

    private fun clearErrors() {
        binding.tfUpdateMobileNumber.error = null
    }
}