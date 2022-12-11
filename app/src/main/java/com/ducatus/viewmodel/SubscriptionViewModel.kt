package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ducatus.utils.EventWrapper
import com.ducatus.data.Subscription

class SubscriptionViewModel : ViewModel() {
    private val mutableSubscription = MutableLiveData<EventWrapper<Subscription>>()
    val subscription: LiveData<EventWrapper<Subscription>> get() = mutableSubscription

    fun setSubscription(subscription: Subscription) {
        mutableSubscription.value = EventWrapper(subscription)
    }
}