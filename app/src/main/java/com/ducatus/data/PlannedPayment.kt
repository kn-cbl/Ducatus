package com.ducatus.data

data class PlannedPayment(
    val id: String? = null, // category id
    val name: String? = null,
    val nameLower: String? = null,
    val amount: Double = 0.0,
    var payment_type: String? = null,
    val frequency: Int = 0,
    val date: Long? = null,
    val notifications: Int = 0,
    val recurrence: Int = 0,
    var notes: String? = null,
    val categoryName: String? = null,
    val categoryNameLower: String? = null,
    val categoryColor: String? = null,
    val categoryIcon: String? = null,
    val subcategoryId: String? = null,
    val subcategoryName: String? = null,
    val subcategoryColor: String? = null,
    val subcategoryIcon: String? = null,
)
