package com.ducatus.data

data class Loan(
    var id: String? = null,
    var name: String? = null,
    var nameLower: String? = null,
    var amount: Double = 0.0,
    var type: String? = null,
    var dueDate: Long? = null,
    var paidAt: Long? = null,
    var notes: String? = null,
    var notesLower: String? = null,
    var notificationId: Int? = null,
)
