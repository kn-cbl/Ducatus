package com.ducatus.data

data class Challenge(
    val id: Int = 0,
    var title: String = "",
    var duration: Int = 0,
    var challengeAmount: Int = 0,
    var savedAmount: Int = 0,
    var dateStarted: Long? = null,
    var isFinished: Boolean = false,
)