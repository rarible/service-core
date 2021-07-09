package com.rarible.core.reduce.queue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LimitedQueueTest {

    @Test
    fun `should saved only limited amount of elements`() {
        val limitedQueue = LimitedQueue<String>(4)

        val element1 = "1"
        val element2 = "2"
        val element3 = "3"
        val element4 = "4"
        val element5 = "5"
        val element6 = "6"

        listOf(element1, element2, element3, element4, element5, element6).forEach {
            limitedQueue.push(it)
        }

        val elementList = limitedQueue.getElementList()
        assertThat(elementList).hasSize(4)

        assertThat(elementList[0]).isEqualTo("6")
        assertThat(elementList[1]).isEqualTo("5")
        assertThat(elementList[2]).isEqualTo("4")
        assertThat(elementList[3]).isEqualTo("3")
    }
}