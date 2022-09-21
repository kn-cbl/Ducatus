package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ducatus.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SettingsFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var activity: Activity
    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity()
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rlUserProfile.setOnClickListener {
            activityIntent(Intent(activity, UserProfileActivity::class.java), false)
        }

        binding.rlUpdatePassword.setOnClickListener {
            activityIntent(Intent(activity, UpdatePasswordActivity::class.java), false)
        }

        binding.rlPrivacy.setOnClickListener {
            activityIntent(Intent(activity, PrivacyActivity::class.java), false)
        }

        binding.rlAboutApp.setOnClickListener {
//            val action = SettingsFragmentDirections.actionSettingsFragmentToAboutAppFragment()
//            findNavController().navigate(action)

            activityIntent(Intent(activity, AboutAppActivity::class.java), false)
        }

        binding.rlLogout.setOnClickListener {
            activityIntent(Intent(activity, LoginActivity::class.java), true)
        }
    }

    private fun activityIntent(intent: Intent, finish: Boolean) {
        startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        if (finish) {
            auth = Firebase.auth
            auth.signOut()

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            activity.finish()
        }
    }
}