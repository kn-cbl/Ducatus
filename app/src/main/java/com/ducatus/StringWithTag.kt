package com.ducatus

class StringWithTag(stringPart: String, tagPart: String, tagPart2: String?, tagPart3: String?) {
    var string = stringPart
    var tag = tagPart
    var tag2 = tagPart2
    var tag3 = tagPart3

    override fun toString(): String {
        return string
    }
}