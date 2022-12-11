package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ducatus.utils.EventWrapper

class AmountViewModel : ViewModel() {
    private val mutableAmount = MutableLiveData<EventWrapper<String>>()
    val amount: LiveData<EventWrapper<String>> get() = mutableAmount

    fun setAmount(amount: String) {
        mutableAmount.value = EventWrapper(amount)
    }
}