package com.ducatus.data

data class Category(
    val category_id: String? = null,
    val category_name: String? = null,
    val category_nature: Int = 0,
    val category_color: String? = null,
    val category_icon: String? = null,
    val category_allocated: Boolean = false
)