package com.ducatus

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.Navigation
import com.ducatus.databinding.ActivityAccountsBinding

class AccountsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.tbAccounts.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        val navController = Navigation.findNavController(this, R.id.fcAccounts)
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
        else {
            super.onBackPressed()
        }
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}