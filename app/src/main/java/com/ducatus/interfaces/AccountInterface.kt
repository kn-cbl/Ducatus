package com.ducatus.interfaces

import android.view.View

interface AccountInterface {
    fun showPopup(view: View, menu: Int, accountId: String)
}