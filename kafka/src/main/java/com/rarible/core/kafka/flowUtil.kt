package com.rarible.core.kafka

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/*
TODO: Kotlin does not support Flow<T>.chunked operator yet
 https://github.com/Kotlin/kotlinx.coroutines/issues/1302
 */
@Deprecated("Do NOT use it, works with bugs")
fun <T> Flow<T>.chunked(maxSize: Int, delayMillis: Long = 0): Flow<List<T>> {
    val buffer = Channel<T>(maxSize)
    return channelFlow {
        coroutineScope {
            launch {
                this@chunked.collect {
                    // Will suspend if the buffer is full.
                    buffer.send(it)
                }
                buffer.close()
            }
            launch {
                while (!buffer.isClosedForReceive) {
                    delay(delayMillis)
                    val chunk = getChunk(buffer, maxSize)
                    this@channelFlow.send(chunk)
                }
            }
        }
    }
}

private suspend fun <T> getChunk(buffer: Channel<T>, maxChunkSize: Int): List<T> {
    // Receive at least one element from the buffer.
    val received = buffer.receive()
    val chunk = arrayListOf(received)
    while (chunk.size < maxChunkSize) {
        val tryReceive = buffer.tryReceive()
        chunk += tryReceive.getOrNull() ?: return chunk
    }
    return chunk
}
