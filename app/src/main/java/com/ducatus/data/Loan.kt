package com.ducatus.data

data class Loan(
    val id: String? = null,
    val name: String? = null,
    val amount: Double = 0.0,
    val type: Int = 0,
    val date: Long? = null,
    val hour: Long? = null,
    val minute: Long? = null,
    val notes: String? = null,
    val status: Int = 0,
)
