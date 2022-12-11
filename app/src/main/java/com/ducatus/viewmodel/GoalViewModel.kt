package com.ducatus.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ducatus.utils.EventWrapper
import com.ducatus.data.Goal

class GoalViewModel : ViewModel() {
    private val mutableGoal = MutableLiveData<EventWrapper<Goal>>()
    val goal: LiveData<EventWrapper<Goal>> get() = mutableGoal

    fun setGoal(goal: Goal) {
        mutableGoal.value = EventWrapper(goal)
    }
}