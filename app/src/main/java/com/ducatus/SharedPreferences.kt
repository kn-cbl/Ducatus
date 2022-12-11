package com.ducatus

import android.content.Context
import android.content.SharedPreferences

class SharedPreferences(context: Context) {
    private val currentAccountId: String = "0"
    private val currentAccountName: String = "Username"
    private val currentAccountColor: String = "green_primary"
    private val currentChallengesChannelId: String = "challenges_channel_0"
    private val currentExpensesChannelId: String = "expenses_channel_0"
    private val currentLoansChannelId: String = "loans_channel_0"
    private val currentSubscriptionsChannelId: String = "subscriptions_channel_0"

    private val customPreferences: SharedPreferences = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

    var accountId: String?
        get() = customPreferences.getString(currentAccountId, "")
        set(value) = customPreferences.edit().putString(currentAccountId, value).apply()

    var accountName: String?
        get() = customPreferences.getString(currentAccountName, "Username")
        set(value) = customPreferences.edit().putString(currentAccountName, value).apply()

    var accountColor: String?
        get() = customPreferences.getString(currentAccountColor, "green_primary")
        set(value) = customPreferences.edit().putString(currentAccountColor, value).apply()

    var challengesChannelId: String?
        get() = customPreferences.getString(currentChallengesChannelId, "challenges_channel_0")
        set(value) = customPreferences.edit().putString(currentChallengesChannelId, value).apply()

    var expensesChannelId: String?
        get() = customPreferences.getString(currentExpensesChannelId, "expenses_channel_0")
        set(value) = customPreferences.edit().putString(currentExpensesChannelId, value).apply()

    var loansChannelId: String?
        get() = customPreferences.getString(currentLoansChannelId, "loans_channel_0")
        set(value) = customPreferences.edit().putString(currentLoansChannelId, value).apply()

    var subscriptionsChannelId: String?
        get() = customPreferences.getString(currentSubscriptionsChannelId, "subscriptions_channel_0")
        set(value) = customPreferences.edit().putString(currentSubscriptionsChannelId, value).apply()

}