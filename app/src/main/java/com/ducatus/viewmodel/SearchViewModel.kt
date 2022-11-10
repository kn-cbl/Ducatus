package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SearchViewModel : ViewModel() {
    private val mutableSearchInput = MutableLiveData<String>()
    val searchInput: LiveData<String> get() = mutableSearchInput

    fun searchName(name: String) {
        mutableSearchInput.value = name
    }
}