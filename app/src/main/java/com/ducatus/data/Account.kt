package com.ducatus.data

data class Account(
    val id: String? = null,
    val name: String? = null,
    val nameLower: String? = null,
    val color: String? = null,
    val monthlyBudget: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val selected: Boolean = false,
)