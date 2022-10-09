package com.ducatus

import java.sql.Date
import java.sql.Timestamp

data class Budget(
    val budget_id: Int? = null,
    val budget_name: String? = null,
    val budget_amount_total: Double? = null,
    val budget_amount_spent: Double? = null,
    val budget_created_at: Date? = null,
    val account_id: Int? = null,
    val category_id: Int? = null,
)