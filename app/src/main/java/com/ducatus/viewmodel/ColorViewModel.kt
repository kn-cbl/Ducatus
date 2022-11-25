package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ColorViewModel : ViewModel() {
    private val mutableColor = MutableLiveData<String>()
    val color: LiveData<String> get() = mutableColor

    fun setColor(color: String) {
        mutableColor.value = color
    }
}