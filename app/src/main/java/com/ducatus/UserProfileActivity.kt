package com.ducatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.app.NavUtils
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.ducatus.databinding.ActivityUserProfileBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class UserProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.tbUserProfile.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        val navController = Navigation.findNavController(this, R.id.fcUserProfile)
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
        else {
            super.onBackPressed()
        }
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}