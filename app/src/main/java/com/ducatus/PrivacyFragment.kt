package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentPrivacyBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class PrivacyFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentPrivacyBinding
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var toolbar: MaterialToolbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.clPrivacy)
        toolbar = activity.findViewById(R.id.tbPrivacy)
        toolbar.title = getString(R.string.privacy)

        binding = FragmentPrivacyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.llPrivacyPolicy.setOnClickListener {
            toolbar.title = getString(R.string.privacy_policy)
            val action = PrivacyFragmentDirections.actionPrivacyFragmentToPrivacyPolicyFragment()
            findNavController().navigate(action)
        }

        binding.llTOS.setOnClickListener {
            toolbar.title = getString(R.string.terms_of_service)
            val action = PrivacyFragmentDirections.actionPrivacyFragmentToTermsOfServiceFragment()
            findNavController().navigate(action)
        }

        binding.llResetUserData.setOnClickListener {
            toolbar.title = getString(R.string.reset_data)
            val action = PrivacyFragmentDirections.actionPrivacyFragmentToResetUserDataFragment()
            findNavController().navigate(action)
        }

        binding.llDeleteUserData.setOnClickListener {
            toolbar.title = getString(R.string.delete_user_account)
            val action = PrivacyFragmentDirections.actionPrivacyFragmentToDeleteUserAccountFragment()
            findNavController().navigate(action)
        }
    }
}