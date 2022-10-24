package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class IconViewModel : ViewModel() {
    private val mutableSelectedIcon = MutableLiveData<String>()
    val selectedIcon: LiveData<String> get() = mutableSelectedIcon

    fun selectIcon(icon: String) {
        mutableSelectedIcon.value = icon
    }
}