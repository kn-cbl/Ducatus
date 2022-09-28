package com.ducatus

import java.math.BigDecimal

data class Account(
    val account_id: Int? = null,
    val account_name: String? = null,
    val account_monthly_budget: Double? = null,
    val account_color: String? = null,
)