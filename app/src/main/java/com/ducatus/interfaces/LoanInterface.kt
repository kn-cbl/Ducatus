package com.ducatus.interfaces

import android.app.Activity
import com.ducatus.data.Loan

interface LoanInterface {
    fun getActivityInterface(): Activity
    fun viewItem(loanId: String)
}