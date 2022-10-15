package com.ducatus

data class Account(
    val account_id: Int? = null,
    val account_name: String? = null,
    val account_color: String? = null,
    val account_monthly_budget: Double = 0.0,
    val account_remaining_budget: Double = 0.0,
    val selected: Boolean = false,
)