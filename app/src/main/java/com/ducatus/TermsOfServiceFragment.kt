package com.ducatus

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ducatus.databinding.FragmentTermsOfServiceBinding

class TermsOfServiceFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentTermsOfServiceBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentTermsOfServiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnTOSDecline.setOnClickListener {
            activity.onBackPressed()
        }

        binding.btnTOSAgree.setOnClickListener {
            activity.onBackPressed()
        }
    }
}