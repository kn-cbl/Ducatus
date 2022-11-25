package com.ducatus.data

data class GoalHistory(
    var accountID: String = "",
    var goalkey: String = "",
    var goalHistoryKey: String = "",
    var datePaid: String = "",
    var timePaid: String = "",
    var amountPaid: Double = 0.0
)
