package com.ducatus

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.Navigation
import com.ducatus.databinding.ActivityUserProfileBinding

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