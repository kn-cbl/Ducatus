package com.ducatus.interfaces

import android.app.Activity
import com.ducatus.data.Transaction

interface TransactionInterface {
    fun getActivityInterface(): Activity
    fun viewItem(transaction: Transaction)
}