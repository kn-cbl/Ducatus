package com.ducatus.data

data class UserNotification(
    var id: String? = null,
    var type: String? = null,
    var title: String? = null,
    var message: String? = null,
    var itemId: String? = null,
    var notifiedAt: Long? = null,
)
