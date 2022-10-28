package com.ducatus

import android.app.Activity

interface TransactionInterface {
    fun getActivityInterface(): Activity
    fun viewItem(transactionId: String)
}