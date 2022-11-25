package com.ducatus

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ducatus.databinding.FragmentSettingsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SettingsFragment : Fragment() {
    private lateinit var activity: Activity
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var gso: GoogleSignInOptions
    private lateinit var gsc: GoogleSignInClient

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
        isGoogleOnly()

        binding.rlUserProfile.setOnClickListener {
            activityIntent(Intent(activity, UserProfileActivity::class.java), false)
        }

        binding.rlUpdatePassword.setOnClickListener {
            activityIntent(Intent(activity, UpdatePasswordActivity::class.java), false)
        }

        binding.rlAccounts.setOnClickListener {
            activityIntent(Intent(activity, AccountsActivity::class.java), false)
        }

        binding.rlCategories.setOnClickListener {
            activityIntent(Intent(activity, CategoriesActivity::class.java), false)
        }

        binding.rlNotifications.setOnClickListener {
            if (Build.VERSION.SDK_INT <= 26) {
                manageNotifications()
            }
            else {
                activityIntent(Intent(activity, NotificationSettingsActivity::class.java), false)
            }
        }

        binding.rlPrivacy.setOnClickListener {
            activityIntent(Intent(activity, PrivacyActivity::class.java), false)
        }

        binding.rlAboutApp.setOnClickListener {
            activityIntent(Intent(activity, AboutAppActivity::class.java), false)
        }

        binding.rlLogout.setOnClickListener {
            activityIntent(Intent(activity, LoginActivity::class.java), true)
        }
    }

    private fun isGoogleOnly() {
        val auth = Firebase.auth
        val firebaseUser: FirebaseUser? = auth.currentUser
        if (firebaseUser != null) {
            val providers = mutableListOf<String>()
            for (item in firebaseUser.providerData) {
                providers.add(item.providerId)
            }

            if (!providers.contains("password")) {
                binding.rlUpdatePassword.visibility = View.GONE
            }
        }
    }

    private fun manageNotifications() {
        val notificationIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        notificationIntent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        startActivity(notificationIntent)
    }

    private fun activityIntent(intent: Intent, finish: Boolean) {
        startActivity(intent)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        if (finish) {
            val firebaseUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                FirebaseAuth.getInstance().signOut()

                gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build()

                gsc = GoogleSignIn.getClient(requireActivity(), gso)

                val googleSignInAccount: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(activity)
                if (googleSignInAccount != null) {
                    gsc.signOut()
                }

                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                activity.finish()
            }
        }
    }
}