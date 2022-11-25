package com.ducatus.interfaces

import android.app.Activity
import com.ducatus.data.SubscriptionHistory

interface SubscriptionHistoryInterface {
    fun getActivityInterface(): Activity
    fun confirmPayment(subscriptionHistory: SubscriptionHistory)
}