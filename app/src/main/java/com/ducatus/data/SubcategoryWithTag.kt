package com.ducatus.data

class SubcategoryWithTag(nameTag: String, subcategoryTag: Subcategory) {
    var name = nameTag
    var subcategory = subcategoryTag

    override fun toString(): String {
        return name
    }
}