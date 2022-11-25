package com.ducatus.data

data class Transaction(
    var id: String? = null,
    var name: String? = null,
    var nameLower: String? = null,
    val amount: Double = 0.0,
    val type: Int = 0,
    var paymentType: String? = null,
    var notes: String? = null,
    var imagePath: String? = null,
    var date: Long? = null,
    val dateString: String? = null,
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
