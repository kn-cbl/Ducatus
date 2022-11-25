package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ducatus.EventWrapper

class SearchViewModel2 : ViewModel() {
    private val mutableSearchInput = MutableLiveData<EventWrapper<String>>()
    val searchInput: LiveData<EventWrapper<String>> get() = mutableSearchInput

    fun searchName(name: String) {
        mutableSearchInput.value = EventWrapper(name)
    }
}