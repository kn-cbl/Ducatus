package com.ducatus.data

data class User (
    val email: String? = null,
    val password: String? = null,
    val username: String? = null,
    val mobileNumber: String? = null,
    val enabled: Boolean = true,
)