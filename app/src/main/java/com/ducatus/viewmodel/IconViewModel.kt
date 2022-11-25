package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class IconViewModel : ViewModel() {
    private val mutableIcon = MutableLiveData<String>()
    val icon: LiveData<String> get() = mutableIcon

    fun setIcon(icon: String) {
        mutableIcon.value = icon
    }
}