package com.ducatus.data

data class ChallengeHistory(
    var key: String = "",
    var accountID: String = "",
    var challengeName: String = "",
    var isFinished: Boolean = false,
    var datePaid: String = "",
    var timePaid: String = "",
    var amount: Int = 0,
    var valueIndex: Int = 0
)
