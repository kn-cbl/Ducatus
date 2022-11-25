package com.ducatus.data

import java.sql.Date
import java.sql.Timestamp

data class Budget(
    val id: String? = null, // category id
    var amountTotal: Double = 0.0,
    var amountSpent: Double = 0.0,
    val createdAt: Long? = null,
    val categoryName: String? = null,
    val categoryNameLower: String? = null,
    val categoryColor: String? = null,
    val categoryIcon: String? = null,
    var updatedAt: Long? = null,
)