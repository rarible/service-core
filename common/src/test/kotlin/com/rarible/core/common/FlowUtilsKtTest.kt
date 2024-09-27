package com.rarible.core.common

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.Executors
import kotlin.random.Random

class FlowUtilsKtTest {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun groupConsecutiveBy() = runBlocking<Unit> {
        assertThat(
            flowOf(
                "a" to 1,
                "b" to 1,
                "b" to 2,
                "c" to 1,
                "d" to 1,
                "d" to 2
            ).groupConsecutiveBy { it.first }
                .toList()
        ).containsExactly(
            listOf("a" to 1),
            listOf("b" to 1, "b" to 2),
            listOf("c" to 1),
            listOf("d" to 1, "d" to 2),
        )
    }

    @Test
    fun `groupConsecutiveBy for empty flow`() = runBlocking<Unit> {
        assertThat(
            emptyFlow<Pair<String, Int>>()
                .groupConsecutiveBy { it.first }
                .toList()
        ).isEmpty()
    }

    @Test
    fun `groupConsecutiveBy for flow of one element`() = runBlocking<Unit> {
        assertThat(
            flowOf("a" to 1)
                .groupConsecutiveBy { it.first }
                .toList()
        ).containsExactly(
            listOf("a" to 1)
        )
    }

    @Test
    fun chunked() = runBlocking<Unit> {
        assertThat((1..3).asFlow().chunked(2).toList()).containsExactly(
            listOf(1, 2),
            listOf(3),
        )
    }

    @Test
    fun `chunked with chunk size a divisor of flow size`() = runBlocking<Unit> {
        assertThat((1..4).asFlow().chunked(2).toList()).containsExactly(
            listOf(1, 2),
            listOf(3, 4)
        )
    }

    @Test
    fun `chunked of empty flow is empty`() = runBlocking<Unit> {
        assertThat(emptyFlow<Int>().chunked(2).toList()).isEmpty()
    }

    @Test
    fun `chunkSize is 1`() = runBlocking<Unit> {
        assertThat(flowOf(1, 2, 3).chunked(1).toList()).containsExactly(
            listOf(1),
            listOf(2),
            listOf(3)
        )
    }

    @Test
    fun mapAsync() = runBlocking<Unit> {
        val flowSize = 100
        val taskLengthMinMs = 50L
        val taskLengthMaxMs = 100L
        val concurrency = 5
        // Excessive parallelism to check if we really are limiting concurrency
        val dispatcher = Executors.newFixedThreadPool(concurrency * 2).asCoroutineDispatcher()
        val results = (1..flowSize).asFlow().mapAsync(concurrency) { n ->
            logger.info("task #$n started")
            val start = Instant.now()
            delay(Random.nextLong(taskLengthMinMs, taskLengthMaxMs))
            val end = Instant.now()
            logger.info("task #$n ended")
            ExecutionTime(start, end, n)
        }
            .flowOn(dispatcher)
            .onEach { executionTime ->
                logger.info("mapAsync produced $executionTime")
            }
            .toList()
        assertThat(results).hasSize(flowSize)
        assertThat(results.map { it.n }).containsExactlyElementsOf(1..flowSize)
        // A rough verification that at any point in time, there were only 5 jobs present
        val earliestTaskStart = results.minOf { it.start }
        val latestTaskEnd = results.maxOf { it.end }
        generateSequence(earliestTaskStart) { t -> t.plusMillis(taskLengthMinMs / 2) }
            .takeWhile { t -> t <= latestTaskEnd }
            .forEach { t ->
                val runningTasks = results.filter { it.contains(t) }
                assertThat(runningTasks).hasSizeLessThanOrEqualTo(concurrency)
            }

        dispatcher.close()
    }

    @Test
    fun `mapAsync finishes with exception if callback throws`() = runBlocking<Unit> {
        assertThrows<IllegalArgumentException> {
            (1..10).asFlow().mapAsync(2) { n ->
                if (n == 8) throw IllegalArgumentException("foo")
            }.collect()
        }
    }

    @Test
    fun `mapAsync with concurrencyLimit 1`() = runBlocking<Unit> {
        assertThat(
            (1..10).asFlow().mapAsync(1) { delay(100); it }.toList()
        ).containsExactlyElementsOf(1..10)
    }

    @Test
    fun mapAsyncUnordered() = runBlocking<Unit> {
        val flowSize = 100
        val taskLengthMinMs = 50L
        val taskLengthMaxMs = 100L
        val concurrency = 5
        // Excessive parallelism to check if we really are limiting concurrency
        val dispatcher = Executors.newFixedThreadPool(concurrency * 2).asCoroutineDispatcher()
        val results = (1..flowSize).asFlow().mapAsyncUnordered(concurrency) { n ->
            logger.info("task #$n started")
            val start = Instant.now()
            delay(Random.nextLong(taskLengthMinMs, taskLengthMaxMs))
            val end = Instant.now()
            logger.info("task #$n ended")
            ExecutionTime(start, end, n)
        }
            .flowOn(dispatcher)
            .onEach { executionTime ->
                logger.info("mapAsyncUnordered produced $executionTime")
            }
            .toList()
        assertThat(results).hasSize(flowSize)
        assertThat(results.map { it.n }).containsExactlyInAnyOrderElementsOf(1..flowSize)
        // A rough verification that at any point in time, there were only 5 jobs present
        val earliestTaskStart = results.minOf { it.start }
        val latestTaskEnd = results.maxOf { it.end }
        generateSequence(earliestTaskStart) { t -> t.plusMillis(taskLengthMinMs / 2) }
            .takeWhile { t -> t <= latestTaskEnd }
            .forEach { t ->
                val runningTasks = results.filter { it.contains(t) }
                assertThat(runningTasks).hasSizeLessThanOrEqualTo(concurrency)
            }

        dispatcher.close()
    }
}

data class ExecutionTime(
    val start: Instant,
    val end: Instant,
    val n: Int
) {
    fun contains(t: Instant) = t in start .. end && t != end
}
