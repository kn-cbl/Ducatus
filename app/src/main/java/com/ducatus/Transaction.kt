package com.ducatus

data class Transaction(
    val transaction_id: Int? = null,
    val transaction_name: String? = null,
    val transaction_amount: Double = 0.0,
    val transaction_type: Int = 0,
    val transaction_payment_type: String? = null,
    val transaction_notes: String? = null,
    val transaction_receipt: String? = null,
    val transaction_date: String? = null,
    val transaction_time: String? = null,
)
