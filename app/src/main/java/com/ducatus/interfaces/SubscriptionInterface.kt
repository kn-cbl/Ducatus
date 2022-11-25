package com.ducatus.interfaces

import android.app.Activity
import com.ducatus.data.Subscription

interface SubscriptionInterface {
    fun getActivityInterface(): Activity
    fun viewItem(subscriptionId: String)
}