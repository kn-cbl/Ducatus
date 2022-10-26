package com.ducatus

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.Navigation
import com.ducatus.databinding.ActivityPrivacyBinding

class PrivacyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.tbPrivacy.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        val navController = Navigation.findNavController(this, R.id.fcPrivacy)
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
        else {
            super.onBackPressed()
        }
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}