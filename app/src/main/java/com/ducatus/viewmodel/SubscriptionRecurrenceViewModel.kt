package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SubscriptionRecurrenceViewModel : ViewModel() {
    private val mutableRecurrence = MutableLiveData<Int>()
    val recurrence: LiveData<Int> get() = mutableRecurrence

    fun setRecurrence(recurrence: Int) {
        mutableRecurrence.value = recurrence
    }
}