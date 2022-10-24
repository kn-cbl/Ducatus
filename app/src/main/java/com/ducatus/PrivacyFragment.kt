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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class PrivacyFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: FragmentPrivacyBinding
    private lateinit var rootLayout: ConstraintLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        rootLayout = activity.findViewById(R.id.clPrivacy)

        binding = FragmentPrivacyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rlPrivacyPolicy.setOnClickListener {
            val action = PrivacyFragmentDirections.actionPrivacyFragmentToPrivacyPolicyFragment()
            findNavController().navigate(action)
        }

        binding.rlTOS.setOnClickListener {
            val action= PrivacyFragmentDirections.actionPrivacyFragmentToTermsOfServiceFragment()
            findNavController().navigate(action)
        }

        binding.rlDeactivateAccount.setOnClickListener {
            confirmDeactivate()
        }

        binding.rlDeleteUserData.setOnClickListener {
            confirmDelete()
        }
    }

    private fun confirmDeactivate() {
        MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.deactivate_account_mark))
            .setMessage(resources.getString(R.string.deactivate_account_message))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ -> deactivateAccount() }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .show()
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(activity)
            .setTitle(resources.getString(R.string.delete_user_data_mark))
            .setMessage(resources.getString(R.string.delete_user_data_message))
            .setPositiveButton(resources.getString(R.string.delete)) { _, _ -> deleteUserData() }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
            .show()
    }

    private fun deactivateAccount() {

    }

    private fun deleteUserData() {
        val action = PrivacyFragmentDirections.actionPrivacyFragmentToDeleteUserDataDialogFragment("delete app user")
        findNavController().navigate(action)
    }

    private fun sessionExpired() {
        Snackbar
            .make(rootLayout, getString(R.string.session_expired), Snackbar.LENGTH_LONG)
            .show()

        // add 3 second delay
        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // do nothing
            }
            override fun onFinish() {
                try {
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity.finish()
                }
                catch (e: Exception) {}
            }
        }.start()
    }
}