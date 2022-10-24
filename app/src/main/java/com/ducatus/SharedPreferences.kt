package com.ducatus

import android.content.Context
import android.content.SharedPreferences

class SharedPreferences(context: Context) {
    private val currentAccountId: String = "0"
    private val currentAccountName: String = "Username"
    private val currentAccountColor: String = "green_primary"

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
}