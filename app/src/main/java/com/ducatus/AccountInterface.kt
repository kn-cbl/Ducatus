package com.ducatus

import android.app.Activity
import android.view.View

interface AccountInterface {
    fun getActivityInterface(): Activity
    fun showPopup(view: View, menu: Int)
}