package com.ducatus.interfaces

import com.ducatus.data.Goals
import java.lang.Exception

interface FirebaseDatabaseCallback {

    fun onSuccessInsert()
    fun onError(e: Exception)
    fun onSuccessListOfGoals(goalsList: List<Goals>)
}