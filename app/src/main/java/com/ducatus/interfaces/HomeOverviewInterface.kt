package com.ducatus.interfaces

import android.app.Activity

interface HomeOverviewInterface {
    fun getActivityInterface(): Activity
    fun viewItem(type: Char, item: String)
}