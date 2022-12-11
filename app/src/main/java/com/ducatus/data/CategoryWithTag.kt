package com.ducatus.data

import com.ducatus.data.Category

class CategoryWithTag(nameTag: String, categoryTag: Category) {
    var name = nameTag
    var category = categoryTag

    override fun toString(): String {
        return name
    }
}