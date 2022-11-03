package com.ducatus

import android.app.Activity

interface LoanInterface {
    fun getActivityInterface(): Activity
    fun viewItem(loanId: String)
}