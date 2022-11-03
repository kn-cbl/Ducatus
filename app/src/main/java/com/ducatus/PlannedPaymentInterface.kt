package com.ducatus

import android.app.Activity

interface PlannedPaymentInterface {
    fun getActivityInterface(): Activity
    fun viewItem(plannedPaymentId: String)
}