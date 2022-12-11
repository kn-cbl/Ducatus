package com.ducatus.interfaces

import android.view.View

interface CategoryInterface {
    fun showPopup(view: View, position: Int, categoryId: String)
}