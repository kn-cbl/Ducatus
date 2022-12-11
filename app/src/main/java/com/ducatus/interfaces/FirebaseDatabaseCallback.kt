package com.ducatus.interfaces

import com.ducatus.data.ChallengeHistory
import com.ducatus.data.GoalHistory
import com.ducatus.data.Goals
import java.lang.Exception

interface FirebaseDatabaseCallback {

    fun onSuccessInsert(key: String) {
        /* Default empty implementation*/
    }

    fun onError(e: Exception) {
        /* Default empty implementation*/
    }

    fun onSuccessListOfGoals(goalsList: List<Goals>) {
        /* Default empty implementation*/
    }

    fun onSuccessListOfGoalHistory(goalHistoryList: List<GoalHistory>) {
        /* Default empty implementation*/
    }

    fun onSuccessListOfChallengeHistory(chList: List<ChallengeHistory>) {
        /* Default empty implementation*/
    }

    fun onSuccessDelete() {
        /**
         * Default empty implementation
         */
    }
}