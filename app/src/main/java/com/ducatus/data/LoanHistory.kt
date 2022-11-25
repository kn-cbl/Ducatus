package com.ducatus.data

data class LoanHistory(
    var id: String? = null,
    val amount: Double = 0.0,
    val type: String? = null,
    val date: Long? = null,
    val notes: String? = null,
    val loanId: String? = null,
)
