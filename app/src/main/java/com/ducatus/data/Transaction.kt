package com.ducatus.data

data class Transaction(
    val id: String? = null,
    val amount: Double = 0.0,
    val type: Int = 0,
    var paymentType: String? = null,
    var notes: String? = null,
    val receipt: String? = null,
    val date: Long? = null,
    val hour: Long? = null,
    val minute: Long? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val categoryNameLower: String? = null,
    val categoryColor: String? = null,
    val categoryIcon: String? = null,
    val subcategoryId: String? = null,
    val subcategoryName: String? = null,
    val subcategoryNameLower: String? = null,
    val subcategoryColor: String? = null,
    val subcategoryIcon: String? = null,
)
