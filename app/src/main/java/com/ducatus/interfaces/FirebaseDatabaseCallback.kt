package com.ducatus.interfaces

import com.ducatus.data.GoalHistory
import com.ducatus.data.Goals
import java.lang.Exception

interface FirebaseDatabaseCallback {

    fun onSuccessInsert(key: String)
    fun onError(e: Exception)
    fun onSuccessListOfGoals(goalsList: List<Goals>)
    fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>)
}