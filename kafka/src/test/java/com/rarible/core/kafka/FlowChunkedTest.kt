package com.rarible.core.kafka

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled // Run manually.
class FlowChunkedTest {
    @Test
    fun `flow chunked`() {
        val range = 0 until 1000
        val flow = range.asFlow().transform {
            emit(it)
            delay(10L)
        }
        runBlocking {
            val all = flow.chunked(maxSize = 20, delayMillis = 100)
                .onEach { println(it) }
                .toList()
                .flatten()
            assertThat(all).isEqualTo(range.toList())
        }
    }
}
