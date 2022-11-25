package com.ducatus

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentDeleteUserAccountConfirmBinding
import com.ducatus.databinding.FragmentResetUserDataConfirmBinding

class ResetUserDataConfirmFragment : Fragment() {
    private lateinit var binding: FragmentResetUserDataConfirmBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentResetUserDataConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnResetUserDataConfirm.setOnClickListener {
            val action = ResetUserDataConfirmFragmentDirections.actionResetUserDataConfirmFragmentToResetUserDataDialogFragment("reset app user")
            findNavController().navigate(action)
        }
    }
}