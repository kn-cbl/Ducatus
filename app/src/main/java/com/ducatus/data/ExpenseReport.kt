package com.ducatus.data

data class ExpenseReport(
    val name: String? = null,
    val amount: Double = 0.0,
    val date: Long? = null,
    val type: Int = 0,
    val paymentType: String? = null,
    val categoryName: String? = null,
    val subcategoryName: String? = null,
)
