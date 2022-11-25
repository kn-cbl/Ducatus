package com.ducatus

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ducatus.databinding.ActivityNotificationSettingsBinding

class NotificationSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationSettingsBinding
    private lateinit var channels: Map<String, String>
    private lateinit var notificationManager: NotificationManager
    private lateinit var sharedPreferences: SharedPreferences
    private var expensesNotificationChannel: NotificationChannel? = null
    private var challengesNotificationChannel: NotificationChannel? = null
    private var loansNotificationChannel: NotificationChannel? = null
    private var subscriptionsNotificationChannel: NotificationChannel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        loadSwitchStates()

        binding.tbNotificationSettings.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.smExpensesReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateNotificationChannel(true, "Expenses")
            }
            else {
                updateNotificationChannel(false, "Expenses")
            }
        }

        binding.smBillsSubscriptionsPaymentReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateNotificationChannel(true, "Subscriptions")
            }
            else {
                updateNotificationChannel(false, "Subscriptions")
            }
        }

        binding.smDebtsLoansReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateNotificationChannel(true, "Loans")
            }
            else {
                updateNotificationChannel(false, "Loans")
            }
        }

        binding.smChallengesReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateNotificationChannel(true, "Challenges")
            }
            else {
                updateNotificationChannel(false, "Challenges")
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun loadSwitchStates() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        sharedPreferences = SharedPreferences(this)
        channels = mapOf(
            "Challenges" to sharedPreferences.challengesChannelId!!,
            "Expenses" to sharedPreferences.expensesChannelId!!,
            "Loans" to sharedPreferences.loansChannelId!!,
            "Subscriptions" to sharedPreferences.subscriptionsChannelId!!
        )

        challengesNotificationChannel = notificationManager.getNotificationChannel(sharedPreferences.challengesChannelId)
        expensesNotificationChannel = notificationManager.getNotificationChannel(sharedPreferences.expensesChannelId)
        loansNotificationChannel = notificationManager.getNotificationChannel(sharedPreferences.loansChannelId)
        subscriptionsNotificationChannel = notificationManager.getNotificationChannel(sharedPreferences.subscriptionsChannelId)

        if (expensesNotificationChannel == null) {
            createNotificationChannel("Expenses")
            expensesNotificationChannel = notificationManager.getNotificationChannel(sharedPreferences.expensesChannelId)
        }
        when (expensesNotificationChannel!!.importance) {
            NotificationManager.IMPORTANCE_DEFAULT -> {
                binding.smExpensesReminder.isChecked = true
            }
            NotificationManager.IMPORTANCE_NONE -> {
                binding.smExpensesReminder.isChecked = false
            }
        }

        if (challengesNotificationChannel == null) {
            createNotificationChannel("Challenges")
            challengesNotificationChannel = notificationManager.getNotificationChannel(sharedPreferences.challengesChannelId)
        }
        when (challengesNotificationChannel!!.importance) {
            NotificationManager.IMPORTANCE_DEFAULT -> {
                binding.smChallengesReminder.isChecked = true
            }
            NotificationManager.IMPORTANCE_NONE -> {
                binding.smChallengesReminder.isChecked = false
            }
        }

        if (loansNotificationChannel == null) {
            createNotificationChannel("Loans")
            loansNotificationChannel = notificationManager.getNotificationChannel(sharedPreferences.loansChannelId)
        }
        when (loansNotificationChannel!!.importance) {
            NotificationManager.IMPORTANCE_DEFAULT -> {
                binding.smDebtsLoansReminder.isChecked = true
            }
            NotificationManager.IMPORTANCE_NONE -> {
                binding.smDebtsLoansReminder.isChecked = false
            }
        }

        if (subscriptionsNotificationChannel == null) {
            createNotificationChannel("Subscriptions")
            subscriptionsNotificationChannel = notificationManager.getNotificationChannel(sharedPreferences.subscriptionsChannelId)
        }
        when (subscriptionsNotificationChannel!!.importance) {
            NotificationManager.IMPORTANCE_DEFAULT -> {
                binding.smBillsSubscriptionsPaymentReminder.isChecked = true
            }
            NotificationManager.IMPORTANCE_NONE -> {
                binding.smBillsSubscriptionsPaymentReminder.isChecked = false
            }
        }
    }

    private fun createNotificationChannel(channel: String) {
        val channelId = channels[channel]
        channelId?.let {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(channelId, channel, importance)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun updateNotificationChannel(enabled: Boolean, channel: String) {
        channels = mapOf(
            "Challenges" to sharedPreferences.challengesChannelId!!,
            "Expenses" to sharedPreferences.expensesChannelId!!,
            "Loans" to sharedPreferences.loansChannelId!!,
            "Subscriptions" to sharedPreferences.subscriptionsChannelId!!
        )

        val channelId = channels[channel]
        channelId?.let {
            notificationManager.deleteNotificationChannel(it)

            when (enabled) {
                true -> {
                    val importance = NotificationManager.IMPORTANCE_DEFAULT

                    // notification channels cannot be updated if current importance
                    // is lower than the to be updated value
                    // to bypass this, get last character of channel id and increment it
                    // to create a new channel id
                    val lastChar = it.last().digitToInt()
                    val newChannelId = it.dropLast(1) + "${lastChar + 1}"
                    updateNotificationChannelId(channel, newChannelId)

                    val notificationChannel = NotificationChannel(newChannelId, channel, importance)
                    notificationManager.createNotificationChannel(notificationChannel)
                }
                else -> {
                    val importance = NotificationManager.IMPORTANCE_NONE
                    val notificationChannel = NotificationChannel(it, channel, importance)
                    notificationManager.createNotificationChannel(notificationChannel)
                }
            }
        }
    }

    private fun updateNotificationChannelId(channel: String, newChannelId: String) {
        when (channel) {
            "Challenges" -> {
                sharedPreferences.challengesChannelId = newChannelId
            }
            "Expenses" -> {
                sharedPreferences.expensesChannelId = newChannelId
            }
            "Loans" -> {
                sharedPreferences.loansChannelId = newChannelId
            }
            "Subscriptions" -> {
                sharedPreferences.subscriptionsChannelId = newChannelId
            }
        }
    }
}