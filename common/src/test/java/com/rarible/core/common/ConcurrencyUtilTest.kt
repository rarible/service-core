package com.rarible.core.common

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConcurrencyUtilTest {
    @Test
    fun `batch handle`() = runBlocking<Unit> {
        val result = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9).asyncBatchHandle(3, ::handle)
        assertThat(result).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9")
    }

    private fun handle(value : Int): String {
        return value.toString()
    }
}