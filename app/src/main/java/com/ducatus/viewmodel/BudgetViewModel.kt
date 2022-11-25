package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ducatus.EventWrapper
import com.ducatus.data.Budget

class BudgetViewModel : ViewModel() {
    private val mutableBudget = MutableLiveData<EventWrapper<Budget>>()
    val budget: LiveData<EventWrapper<Budget>> get() = mutableBudget

    fun setBudget(budget: Budget) {
        mutableBudget.value = EventWrapper(budget)
    }
}