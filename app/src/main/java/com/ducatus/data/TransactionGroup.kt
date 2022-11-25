package com.ducatus.data

import com.ducatus.adapter.TransactionAdapter

data class TransactionGroup(
    val date: Long? = null,
    val amountTotal: Double = 0.0,
    val transactions: List<Transaction>? = null,
    val adapter: TransactionAdapter
)
