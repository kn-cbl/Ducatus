package com.ducatus.interfaces

import com.ducatus.data.SubscriptionHistory

interface SubscriptionHistoryInterface {
    fun confirmPayment(subscriptionHistory: SubscriptionHistory)
}