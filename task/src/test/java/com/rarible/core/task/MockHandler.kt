package com.rarible.core.task

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import org.assertj.core.api.Assertions.assertThat
import java.util.concurrent.ConcurrentHashMap

class MockHandler(
    override val type: String
) : TaskHandler<Int> {
    private val messageChannelsByParam = ConcurrentHashMap<String, Channel<Int>>()

    override fun runLongTask(from: Int?, param: String): Flow<Int> {
        val messages = Channel<Int>(Channel.RENDEZVOUS)
        val oldMessages = messageChannelsByParam.putIfAbsent(param, messages)
        oldMessages?.close(IllegalStateException("previous flow should have been closed for $param"))
        return messages.consumeAsFlow()
    }

    suspend fun sendMessage(param: String, message: Int) {
        val messages = messageChannelsByParam[param]
        assertThat(messages).isNotNull
        messages!!.send(message)
    }

    fun close(param: String, exception: Exception? = null) {
        val messages = messageChannelsByParam[param]
        assertThat(messages).isNotNull
        messages!!.close(exception)
    }
}
