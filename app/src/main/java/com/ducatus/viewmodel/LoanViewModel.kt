package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ducatus.EventWrapper
import com.ducatus.data.Loan

class LoanViewModel : ViewModel() {
    private val mutableLoan = MutableLiveData<EventWrapper<Loan>>()
    val loan: LiveData<EventWrapper<Loan>> get() = mutableLoan

    fun setLoan(loan: Loan) {
        mutableLoan.value = EventWrapper(loan)
    }
}