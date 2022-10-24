package com.ducatus.data

import java.sql.Date
import java.sql.Timestamp

data class Budget(
    val budget_id: String? = null,
    val budget_name: String? = null,
    val budget_amount_total: Double = 0.0,
    val budget_amount_spent: Double = 0.0,
    val budget_created_at: Long? = null,
    val category_id: String? = null,
    val category_name: String? = null,
    val category_color: String? = null,
    val category_icon: String? = null
)