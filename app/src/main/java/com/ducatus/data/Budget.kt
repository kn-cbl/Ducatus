package com.ducatus.data

import java.sql.Date
import java.sql.Timestamp

data class Budget(
    val id: String? = null, // category id
    val name: String? = null,
    val nameLower: String? = null,
    val amountTotal: Double = 0.0,
    val amountSpent: Double = 0.0,
    val createdAt: Long? = null,
    val categoryName: String? = null,
    val categoryColor: String? = null,
    val categoryIcon: String? = null
)