package com.ducatus

import android.app.Activity
import android.view.View

interface SubcategoryInterface {
    fun getActivityInterface(): Activity
    fun showPopup(view: View, position: Int)
}