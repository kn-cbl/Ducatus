package com.ducatus.interfaces

import android.app.Activity
import com.ducatus.data.Budget

interface BudgetInterface {
    fun getActivityInterface(): Activity
    fun viewItem(budget: Budget)
}