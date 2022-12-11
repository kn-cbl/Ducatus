package com.ducatus.utils

open class EventWrapper<out T>(private val content: T) {
    private var handled = false

    fun getContentIfNotHandled(): T? {
        return if (handled) {
            null
        }
        else {
            handled = true
            content
        }
    }

    fun peekContent(): T = content
}