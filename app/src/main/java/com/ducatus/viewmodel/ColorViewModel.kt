package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ColorViewModel : ViewModel() {
    private val mutableSelectedColor = MutableLiveData<String>()
    val selectedColor: LiveData<String> get() = mutableSelectedColor

    fun selectColor(color: String) {
        mutableSelectedColor.value = color
    }
}