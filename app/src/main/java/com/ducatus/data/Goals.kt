package com.ducatus.data

data class Goals(
    val accountID: String = "",
    val goalDescription: String = "",
    val targetDate: String = "",
    val percentage: Double = 0.0,
    val earned: Double = 0.0,
    val remaining: Double = 0.0,
    val goalAmount: Double = 0.0
)
