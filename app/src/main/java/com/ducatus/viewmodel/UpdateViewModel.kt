package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ducatus.EventWrapper

class UpdateViewModel : ViewModel() {
    private val mutableIsUpdated = MutableLiveData<EventWrapper<Boolean>>()
    val isUpdated: LiveData<EventWrapper<Boolean>> get() = mutableIsUpdated

    fun update(updated: Boolean) {
        mutableIsUpdated.value = EventWrapper(updated)
    }
}