package com.ducatus.data

data class Goals(
    var accountID: String = "",
    var key: String = "",
    var goalDescription: String = "",
    var targetDate: String = "",
    var percentage: Double = 0.0,
    var earned: Double = 0.0,
    var remaining: Double = 0.0,
    var goalAmount: Double = 0.0,
    var notes: String = "",
    var color: Int = 0,
    var colorName: String = "",
    var icon: Int = 0,
    var status: Int = 0,
    var dateGoalPaused: String = ""
)
