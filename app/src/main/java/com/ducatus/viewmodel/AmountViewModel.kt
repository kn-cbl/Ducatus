package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AmountViewModel : ViewModel() {
    private val mutableAmount = MutableLiveData<String>()
    val amount: LiveData<String> get() = mutableAmount

    fun setAmount(amount: String) {
        mutableAmount.value = amount
    }
}