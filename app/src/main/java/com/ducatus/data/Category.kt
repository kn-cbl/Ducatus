package com.ducatus.data

data class Category(
    val id: String? = null,
    val name: String? = null,
    val nameLower: String? = null,
    val nature: Int = 0,
    val color: String? = null,
    val icon: String? = null,
    val allocated: Boolean = false
)