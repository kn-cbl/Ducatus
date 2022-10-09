package com.ducatus

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.Navigation
import com.ducatus.databinding.ActivityNotificationsBinding

class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.smExpensesReminder.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked) {
                enableExpensesReminder()
            }
            else {
                disableExpensesReminder()
            }
        }

        binding.smBudgetReminder.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked) {
                enableBudgetReminder()
            }
            else {
                disableBudgetReminder()
            }
        }

        binding.smBillsSubscriptionsPaymentReminder.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked) {
                enableBillsSubscriptionsPaymentReminder()
            }
            else {
                disableBillsSubscriptionsPaymentReminder()
            }
        }

        binding.smDebtsLoansReminder.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked) {
                enableDebtsLoansReminder()
            }
            else {
                disableDebtsLoansReminder()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun enableExpensesReminder() {

    }

    private fun disableExpensesReminder() {

    }

    private fun enableBudgetReminder() {

    }

    private fun disableBudgetReminder() {

    }

    private fun enableBillsSubscriptionsPaymentReminder() {

    }

    private fun disableBillsSubscriptionsPaymentReminder() {

    }

    private fun enableDebtsLoansReminder() {

    }

    private fun disableDebtsLoansReminder() {

    }
}