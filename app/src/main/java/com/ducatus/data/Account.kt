package com.ducatus.data

data class Account(
    val id: String? = null,
    var name: String? = null,
    var nameLower: String? = null,
    var color: String? = null,
    var monthlyBudget: Double = 0.0,
    var remainingBudget: Double = 0.0,
    var remainingBalance: Double = 0.0,
    var budgetRenewsAt: Long? = null,
    val selected: Boolean = false,
)