package com.rarible.core.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GroupingUtilsKtTest {

    @Test
    fun keySet() {
        assertThat(
            listOf(1 to "A", 1 to "B", 2 to "C").groupingBy { it.first }.keySet()
        ).containsExactlyInAnyOrder(1, 2)
        assertThat(
            listOf(1 to "A", 2 to "C", 1 to "B").groupingBy { it.first }.keySet()
        ).containsExactlyInAnyOrder(1, 2)
    }

    @Test
    fun eachFirst() {
        assertThat(
            listOf(1 to "A", 1 to "B", 2 to "C").groupingBy { it.first }.eachFirst()
        ).containsExactlyInAnyOrderEntriesOf(mapOf(
            1 to (1 to "A"),
            2 to (2 to "C"))
        )
        assertThat(
            listOf(1 to "A", 2 to "C", 1 to "B").groupingBy { it.first }.eachFirst()
        ).containsExactlyInAnyOrderEntriesOf(mapOf(
            1 to (1 to "A"),
            2 to (2 to "C"))
        )
    }

    @Test
    fun eachMin() {
        assertThat(
            listOf(1 to "A", 1 to "B", 2 to "C").groupingBy { it.first }.eachMin(compareBy { it.second })
        ).containsExactlyInAnyOrderEntriesOf(mapOf(
            1 to (1 to "A"),
            2 to (2 to "C"))
        )
        assertThat(
            listOf(1 to "B", 1 to "A", 2 to "C").groupingBy { it.first }.eachMin(compareBy { it.second })
        ).containsExactlyInAnyOrderEntriesOf(mapOf(
            1 to (1 to "A"),
            2 to (2 to "C"))
        )
        assertThat(
            listOf(1 to "B", 2 to "C", 1 to "A").groupingBy { it.first }.eachMin(compareBy { it.second })
        ).containsExactlyInAnyOrderEntriesOf(mapOf(
            1 to (1 to "A"),
            2 to (2 to "C"))
        )

        assertThat(
            listOf(1, 2, 3, 4, 5).groupingBy { it / 2 }.eachMin()
        ).containsExactlyInAnyOrderEntriesOf(mapOf(0 to 1, 1 to 2, 2 to 4))
    }
}
