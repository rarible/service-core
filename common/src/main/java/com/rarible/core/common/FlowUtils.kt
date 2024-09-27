package com.rarible.core.common

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.TreeMap

fun <T : Any, K : Any> Flow<T>.groupConsecutiveBy(keySelector: (T) -> K): Flow<List<T>> {
    class LastState(
        val key: K,
        val list: MutableList<T>
    )
    class State(
        val last: LastState?,
        val yieldList: List<T>?
    )

    return this.map<T, T?> { it }
        .onCompletion { emit(null) }
        .scan(State(null, null)) { state: State, element: T? ->
            val key = element?.let { keySelector(it) }
            val lastKey = state.last?.key
            if (key == null) {
                // Last element
                State(null, state.last?.list)
            } else if (lastKey != key) {
                State(LastState(key, mutableListOf(element)), state.last?.list)
            } else {
                State(LastState(key, state.last.list.apply { add(element) }), null)
            }
        }.mapNotNull { it.yieldList }
}

fun <T> Flow<T>.chunked(chunkSize: Int): Flow<List<T>> {
    require(chunkSize > 0) { "Invalid chunkSize $chunkSize" }
    return flow {
        var list = ArrayList<T>(chunkSize)
        var i = 0
        collect { element ->
            if (i == chunkSize) {
                emit(list)
                list = ArrayList(chunkSize)
                i = 0
            }
            list.add(element)
            i += 1
        }
        if (i > 0) emit(list)
    }
}

/**
 * Performs asynchronous operations on the flow with limited concurrency, preserving order.
 * */
fun <T, R> Flow<T>.mapAsync(concurrencyLimit: Int, f: suspend (T) -> R): Flow<R> {
    require(concurrencyLimit > 0) { "Invalid concurrencyLimit $concurrencyLimit" }
    return channelFlow {
        // As results are produced, they are collected by the coordinator coroutine from this channel
        val results = Channel<Pair<Int, R>>(concurrencyLimit)
        // We need to know when the coordinator coroutine should stop waiting for results.
        // This information is available after collect {} finishes,
        // and channelFlow shares this information with the coordinator coroutine at that point.
        val flowSizeChannel = Channel<Int>()
        // Concurrency limit is achieved by requesting permits from a limited-size channel
        val permits = Channel<Unit>(concurrencyLimit)

        // The coordinator coroutine
        launch {
            // To preserve order, we only send the result if its number is equal to the current expected number
            var expectedResultNumber = 0
            val resultSlots = TreeMap<Int, R>()
            var done = 0
            var flowSize = -1
            while (flowSize < 0 || done < flowSize) {
                select<Unit> {
                    results.onReceive { (resultNumber: Int, result: R) ->
                        resultSlots[resultNumber] = result
                        val slotIter = resultSlots.iterator()
                        // Send the results starting from the filled slot until the next empty slot
                        while (slotIter.hasNext()) {
                            val (k, r) = slotIter.next()
                            if (k == expectedResultNumber) {
                                send(r)
                                expectedResultNumber += 1
                                slotIter.remove()
                            } else break
                        }
                        done += 1
                    }
                    flowSizeChannel.onReceive { actualFlowSize ->
                        flowSize = actualFlowSize
                    }
                }
            }
        }
        // Give out initial permits
        repeat(concurrencyLimit) { permits.send(Unit) }
        // Counts the incoming elements
        var i = 0
        collect { element ->
            permits.receive()
            val currentNumber = i++
            launch {
                try {
                    val result = f(element)
                    results.send(currentNumber to result)
                } finally {
                    permits.send(Unit)
                }
            }
        }
        flowSizeChannel.send(i)
    }
}

fun <T, R> Flow<T>.mapAsyncUnordered(concurrencyLimit: Int, f: suspend (T) -> R): Flow<R> {
    require(concurrencyLimit > 0) { "Invalid concurrencyLimit $concurrencyLimit" }
    return channelFlow {
        // Concurrency limit is achieved by requesting permits from a limited-size channel
        val permits = Channel<Unit>(concurrencyLimit)

        // Give out initial permits
        repeat(concurrencyLimit) { permits.send(Unit) }
        collect { element ->
            permits.receive()
            launch {
                try {
                    val result = f(element)
                    send(result)
                } finally {
                    permits.send(Unit)
                }
            }
        }
    }
}
