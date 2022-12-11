package com.ducatus.interfaces

import com.ducatus.data.Transaction

interface TransactionInterface {
    fun viewItem(transaction: Transaction)
}