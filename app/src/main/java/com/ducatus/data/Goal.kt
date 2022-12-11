package com.ducatus.data

data class Goal(
    var id: String = "",
    var name: String = "",
    var nameLower: String = "",
    var targetDate: Long = 0,
    var reachedDate: Long? = null,
    var targetAmount: Double = 0.0,
    var savedAmount: Double = 0.0,
    var color: String = "green_primary",
    var icon: String = "ic_baseline_more_horiz_24",
    var notes: String? = null,
    var status: String = "A",
    var updatedAt: Long? = null,
)
