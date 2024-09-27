package com.rarible.core.common

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.Sets
import org.junit.jupiter.api.Test

class RangeUtilsTest {

    @Test
    fun `split - ok, single`() {
        val heights = Sets.newTreeSet(
            5L,
        )

        val batchSize = 100
        val result = RangeUtils.splitIntoRanges(heights, batchSize)
        assertThat(result).isEqualTo(listOf(5L))
    }

    @Test
    fun `split - ok, several intervals`() {
        val heights = Sets.newTreeSet(
            5L,
            6L, // -> [5,6], 7 excluded
            7L, // -> only 7, 10 is too far
            10L,
            11L, // -> [10, 11], 18 is too far
            18L,
            19L, // -> [18, 19]
        )

        val batchSize = 3
        val result = RangeUtils.splitIntoRanges(heights, batchSize)
        assertThat(result).isEqualTo(
            listOf(
                5L,
                7L,
                10L,
                18L
            )
        )
    }
}
