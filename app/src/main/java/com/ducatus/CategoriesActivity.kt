package com.ducatus

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.Navigation
import com.ducatus.databinding.ActivityCategoriesBinding

class CategoriesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoriesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.tbCategories.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        val navController = Navigation.findNavController(this, R.id.fcCategories)
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
        else {
            super.onBackPressed()
        }
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}