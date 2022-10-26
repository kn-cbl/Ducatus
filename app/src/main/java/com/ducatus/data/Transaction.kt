package com.ducatus.data

data class Transaction(
    val transaction_id: String? = null,
    val transaction_amount: Double = 0.0,
    val transaction_type: Int = 0,
    val transaction_payment_type: String? = null,
    val transaction_notes: String? = null,
    val transaction_receipt: String? = null,
    val transaction_date: Long? = null,
    val transaction_hour: Long? = null,
    val transaction_minute: Long? = null,
    val category_id: String? = null,
    val category_name: String? = null,
    val category_color: String? = null,
    val category_icon: String? = null,
    val subcategory_id: String? = null,
    val subcategory_name: String? = null,
    val subcategory_color: String? = null,
    val subcategory_icon: String? = null
)
