package com.ducatus.data

import com.ducatus.TransactionAdapter

data class TransactionGroup(
    val amountTotal: Double = 0.0,
    val transactions: List<Transaction>? = null,
    val adapter: TransactionAdapter
)
