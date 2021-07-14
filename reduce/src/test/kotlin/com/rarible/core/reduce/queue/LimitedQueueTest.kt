package com.rarible.core.reduce.queue

import com.rarible.core.reduce.model.ReduceSnapshot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LimitedQueueTest {

    @Test
    fun `should saved only limited amount of elements`() {
        val limitedQueue = LimitedSnapshotQueue<TestSnapshot, String, Long, String>(4)

        val element1 = TestSnapshot("1", "1", 1)
        val element2 = TestSnapshot("2", "2", 2)
        val element3 = TestSnapshot("3", "3", 3)
        val element4 = TestSnapshot("4", "4", 4)
        val element5 = TestSnapshot("5", "5", 5)
        val element6 = TestSnapshot("6", "6", 6)

        listOf(element1, element2, element3, element4, element5, element6).forEach {
            limitedQueue.push(it)
        }

        val elementList = limitedQueue.getSnapshotList()
        assertThat(elementList).hasSize(4)

        assertThat(elementList[0].data).isEqualTo("6")
        assertThat(elementList[1].data).isEqualTo("5")
        assertThat(elementList[2].data).isEqualTo("4")
        assertThat(elementList[3].data).isEqualTo("3")
    }

    @Test
    fun `should remove all previous mark from queue`() {
        val limitedQueue = LimitedSnapshotQueue<TestSnapshot, String, Long, String>(10)

        val element1 = TestSnapshot("1", "1", 1)
        val element2 = TestSnapshot("2", "2", 2)
        val element3 = TestSnapshot("3", "3", 2)
        val element4 = TestSnapshot("4", "4", 4)
        val element5 = TestSnapshot("5", "5", 2)
        val element6 = TestSnapshot("6", "6", 6)

        listOf(element1, element2, element3, element4, element5, element6).forEach {
            limitedQueue.push(it)
        }

        val elementList = limitedQueue.getSnapshotList()
        assertThat(elementList).hasSize(4)

        assertThat(elementList[0].data).isEqualTo("6")
        assertThat(elementList[1].data).isEqualTo("5")
        assertThat(elementList[2].data).isEqualTo("4")
        assertThat(elementList[3].data).isEqualTo("1")
    }

    private data class TestSnapshot(
        override val data: String,
        override val id: String,
        override val mark: Long
    ): ReduceSnapshot<String, Long, String>()
}