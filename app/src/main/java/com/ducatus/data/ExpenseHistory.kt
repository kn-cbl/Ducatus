package com.ducatus.data

data class ExpenseHistory(
    val name: String? = null,
    val amount: Double = 0.0,
    val date: Long? = null,
    val type: Char? = null,
    val isExpense: Boolean = true,
    val paymentType: String? = null,
    val imagePath: String? = null,
)