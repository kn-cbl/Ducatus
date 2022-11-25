package com.ducatus.data

data class SubscriptionHistory(
    var id: String? = null,
    val amount: Double = 0.0,
    var dueAt: Long? = null,
    var paidAt: Long? = null,
    val subscriptionId: String? = null,
    val notificationId: Int? = null,
)