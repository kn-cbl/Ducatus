package com.ducatus.data

data class Expense(
    val id: String? = null,
    val gsonObject: String? = null,
    val name: String? = null,
    val amount: Double = 0.0,
    val date: Long? = null,
    val type: Char? = null,
    val paymentType: String? = null,
    val categoryName: String? = null,
    val categoryColor: String? = null,
    val categoryIcon: String? = null,
    val subcategoryName: String? = null,
    val subcategoryColor: String? = null,
    val subcategoryIcon: String? = null,
)