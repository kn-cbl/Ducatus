package com.ducatus

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.ducatus.databinding.FragmentDeleteUserAccountConfirmBinding

class DeleteUserAccountConfirmFragment : Fragment() {
    private lateinit var binding: FragmentDeleteUserAccountConfirmBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeleteUserAccountConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnDeleteUserAccountConfirm.setOnClickListener {
            val action = DeleteUserAccountConfirmFragmentDirections.actionDeleteUserAccountConfirmFragmentToDeleteUserDataDialogFragment("delete app user")
            findNavController().navigate(action)
        }
    }
}