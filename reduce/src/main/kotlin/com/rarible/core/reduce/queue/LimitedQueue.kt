package com.rarible.core.reduce.queue

import java.util.concurrent.ConcurrentLinkedDeque

internal class LimitedQueue<E>(private val limit: Int) {
    private val concurrentLinkedDeque = ConcurrentLinkedDeque<E>()

    fun push(element: E) {
        if (concurrentLinkedDeque.size > limit) {
            concurrentLinkedDeque.removeLast()
        }
        concurrentLinkedDeque.push(element)
    }

    fun getElementList(): List<E> {
        return concurrentLinkedDeque.toList()
    }
}
