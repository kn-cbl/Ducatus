package com.ducatus.data

data class ChallengeHistory(
    val id: String = "",
    var amount: Int = 0,
    var datePaid: Long? = null,
    val position: Int = 0,
    val challengeId: Int = 0,
)
