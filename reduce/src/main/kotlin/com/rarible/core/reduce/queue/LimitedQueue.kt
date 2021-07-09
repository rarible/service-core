package com.rarible.core.reduce.queue

import java.util.concurrent.ConcurrentLinkedDeque

internal class LimitedQueue<E>(private val limit: Int) {
    init {
        require(limit > 0) { "Limit must be positive" }
    }

    private val concurrentLinkedDeque = ConcurrentLinkedDeque<E>()

    fun push(element: E) {
        concurrentLinkedDeque.push(element)

        if (concurrentLinkedDeque.size > limit) {
            concurrentLinkedDeque.removeLast()
        }
    }

    fun getElementList(): List<E> {
        return concurrentLinkedDeque.toList()
    }
}
