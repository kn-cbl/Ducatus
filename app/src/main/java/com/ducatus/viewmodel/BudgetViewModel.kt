package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BudgetViewModel : ViewModel() {
    private val mutableIsUpdated = MutableLiveData<Boolean>()
    val isUpdated: LiveData<Boolean> get() = mutableIsUpdated

    fun update(updated: Boolean) {
        mutableIsUpdated.value = updated
    }
}