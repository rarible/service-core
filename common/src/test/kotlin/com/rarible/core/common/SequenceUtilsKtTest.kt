package com.rarible.core.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import kotlin.random.Random

class SequenceUtilsKtTest {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val seed = java.lang.Long.getLong("testSeed", System.currentTimeMillis())
    private val random = Random(seed)
    init {
        logger.info("Test seed: $seed")
    }

    @Test
    fun mergeSorted() {
        assertThat(
            emptySequence<Int>()
                .mergeSorted(emptySequence<Int>())
                .toList()
        ).isEmpty()
        assertThat(
            emptySequence<Int>()
                .mergeSorted(sequenceOf(10))
                .toList()
        ).containsExactly(10)
        assertThat(
            sequenceOf(20)
                .mergeSorted(emptySequence())
                .toList()
        ).containsExactly(20)
        assertThat(
            sequenceOf(30)
                .mergeSorted(sequenceOf(30))
                .toList()
        ).containsExactly(30, 30)
        assertThat(
            sequenceOf(40)
                .mergeSorted(sequenceOf(41))
                .toList()
        ).containsExactly(40, 41)
        assertThat(
            sequenceOf(51)
                .mergeSorted(sequenceOf(50))
                .toList()
        ).containsExactly(50, 51)
        assertThat(
            sequenceOf(60, 62, 62, 64)
                .mergeSorted(sequenceOf(61, 62, 63, 65))
                .toList()
        ).containsExactly(60, 61, 62, 62, 62, 63, 64, 65)
        assertThat(
            sequenceOf(70)
                .mergeSorted(sequenceOf(71, 72))
                .toList()
        ).containsExactly(70, 71, 72)
        assertThat(
            sequenceOf(81)
                .mergeSorted(sequenceOf(80, 82))
                .toList()
        ).containsExactly(80, 81, 82)
        assertThat(
            sequenceOf(91, 92)
                .mergeSorted(sequenceOf(90))
                .toList()
        ).containsExactly(90, 91, 92)
        assertThat(
            sequenceOf(100, 102)
                .mergeSorted(sequenceOf(101))
                .toList()
        ).containsExactly(100, 101, 102)
        assertThat(
            sequenceOf(110, 110)
                .mergeSorted(sequenceOf(110))
                .toList()
        ).containsExactly(110, 110, 110)
    }

    @RepeatedTest(20)
    fun mergeSorted_random() {
        val length1 = random.nextInt(0, 1000)
        val length2 = random.nextInt(0, 1000)
        val list1 = Array(length1) { random.nextInt() }.sorted()
        val list2 = Array(length2) { random.nextInt() }.sorted()
        val result = list1.asSequence().mergeSorted(list2.asSequence()).toList()
        val expected = (list1 + list2).sorted()
        assertThat(result).containsExactlyElementsOf(expected)
    }

    @Test
    fun sortedUnique() {
        assertThat(emptySequence<Int>().sortedUnique().toList()).isEmpty()
        assertThat(sequenceOf(1).sortedUnique().toList()).containsExactly(1)
        assertThat(sequenceOf(1, 1).sortedUnique().toList()).containsExactly(1)
        assertThat(sequenceOf(1, 2).sortedUnique().toList()).containsExactly(1, 2)
        assertThat(sequenceOf(1, 2, 2).sortedUnique().toList()).containsExactly(1, 2)
    }

    @RepeatedTest(20)
    fun sortedUnique_random() {
        val length = random.nextInt(0, 1000)
        val list = Array(length) { random.nextInt() }.sorted()
        val result = list.asSequence().sortedUnique().toList()
        val expected = list.toSet()
        assertThat(result).containsExactlyElementsOf(expected)
    }
}
