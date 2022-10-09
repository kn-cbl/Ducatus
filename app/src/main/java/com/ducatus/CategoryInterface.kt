package com.ducatus

import android.app.Activity
import android.view.View

interface CategoryInterface {
    fun getActivityInterface(): Activity
    fun showPopup(view: View)
}