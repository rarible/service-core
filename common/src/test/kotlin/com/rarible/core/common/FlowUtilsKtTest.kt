package com.rarible.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.random.Random

const val testTimeoutSeconds = 30L

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
            (1..10).asFlow().mapAsync(1) { delay(10); it }.toList()
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

    @Test
    @Timeout(value = testTimeoutSeconds, unit = TimeUnit.SECONDS)
    @Suppress("UNUSED_PARAMETER")
    fun `takeUntilTimeout should complete on timeout before the end of upstream flow`() = runBlocking<Unit> {
        val delayChannel = Channel<Unit>(RENDEZVOUS)
        suspend fun delayFunction(duration: Duration) = delayChannel.receive()

        val channel = Channel<Int>(4)
        channel.send(1)

        assertThat(
            channel.receiveAsFlow().takeUntilTimeout(Duration.ofSeconds(1), ::delayFunction).map { i ->
                if (i == 2) {
                    // Timeout has finished
                    delayChannel.send(Unit)
                }
                channel.send(i + 1)
                i
            }.toList()
        ).contains(1, 2)
        // the actual completion of takeUntilTimeout is concurrent with the timeout, so we can only check that it stops eventually
    }

    @Test
    @Timeout(value = testTimeoutSeconds, unit = TimeUnit.SECONDS)
    fun `takeUntilTimeout should complete on timeout before the first element of upstream flow`() = runBlocking<Unit> {
        assertThat(
            flow {
                Channel<Unit>().receive()
                emit(1)
            }.takeUntilTimeout(Duration.ofMillis(100)).toList()
        ).isEmpty()
    }

    @Test
    @Timeout(value = testTimeoutSeconds, unit = TimeUnit.SECONDS)
    @Suppress("UNUSED_PARAMETER")
    fun `takeUntilTimeout should complete immediately when upstream completes before timeout`() = runBlocking<Unit> {
        suspend fun infiniteDelay(duration: Duration) = Channel<Unit>().receive()

        assertThat(
            flowOf(1, 2, 3).takeUntilTimeout(Duration.ofSeconds(1), ::infiniteDelay).toList()
        ).containsExactly(1, 2, 3)
    }

    @Test
    @Timeout(value = testTimeoutSeconds, unit = TimeUnit.SECONDS)
    @Suppress("UNUSED_PARAMETER")
    fun `takeUntilTimeout should complete on timeout if the downstream is slow`() = runBlocking<Unit> {
        val delayChannel1 = Channel<Unit>(RENDEZVOUS)
        suspend fun delayFunction1(duration: Duration) = delayChannel1.receive()
        val delayChannel2 = Channel<Unit>(RENDEZVOUS)
        suspend fun delayFunction2(duration: Duration) = delayChannel2.receive()

        val channel1 = Channel<Int>(4)
        channel1.send(1)
        val channel2 = Channel<Int>(4)
        channel2.send(101)

        val list = flow {
            emitAll(channel1.receiveAsFlow().takeUntilTimeout(Duration.ofSeconds(1), ::delayFunction1))
            emitAll(channel2.receiveAsFlow().takeUntilTimeout(Duration.ofSeconds(1), ::delayFunction2))
        }.map { i ->
            when (i) {
                in 1 until 100 -> channel1.send(i + 1)
                else -> channel2.send(i + 1)
            }
            if (i == 2) delayChannel1.send(Unit)
            if (i == 102) delayChannel2.send(Unit)
            i
        }.toList()
        assertThat(list).satisfiesAnyOf(
            Consumer { assertThat(it).containsExactly(1, 2, 101, 102) },
            Consumer { assertThat(it).containsExactly(1, 2, 3, 101, 102) },
            Consumer { assertThat(it).containsExactly(1, 2, 101, 102, 103) },
            Consumer { assertThat(it).containsExactly(1, 2, 3, 101, 102, 103) },
        )
    }

    @Test
    @Timeout(value = testTimeoutSeconds, unit = TimeUnit.SECONDS)
    @Suppress("UNUSED_PARAMETER")
    fun `takeUntilTimeout should complete on timeout if the downstream is slow - 2`() = runBlocking<Unit> {
        val delayChannel = Channel<Unit>(RENDEZVOUS)
        suspend fun delayFunction(duration: Duration) = delayChannel.receive()
        val inMapSignal = Channel<Unit>(RENDEZVOUS)
        val inMapWait = Channel<Unit>(RENDEZVOUS)

        launch {
            assertThat(
                generateSequence(0) { it + 1 }.asFlow()
                    .takeUntilTimeout(Duration.ofSeconds(1), ::delayFunction)
                    .map { i ->
                        if (i == 1) {
                            inMapSignal.send(Unit)
                            inMapWait.receive()
                        }
                        i
                    }
                    .toList()
            ).contains(1)
        }
        launch {
            inMapSignal.receive()
            // the flow should time out before map finishes
            delayChannel.send(Unit)
            delay(100)
            inMapWait.send(Unit)
        }
    }

    class MyException : Exception("kaboom")

    @Test
    @Timeout(value = testTimeoutSeconds, unit = TimeUnit.SECONDS)
    @Suppress("UNUSED_PARAMETER")
    fun `takeUntilTimeout should fail if the upstream fails`() = runBlocking<Unit> {
        suspend fun infiniteDelay(duration: Duration) = Channel<Unit>().receive()

        assertThrows<MyException> {
            flow {
                emit(1)
                throw MyException()
            }.takeUntilTimeout(Duration.ofSeconds(1), ::infiniteDelay)
                .toList()
        }
    }

    @Test
    @Timeout(value = testTimeoutSeconds, unit = TimeUnit.SECONDS)
    @Suppress("UNUSED_PARAMETER")
    fun `takeUntilTimeout should cancel upstream immediately when downstream completes`() = runBlocking<Unit> {
        suspend fun infiniteDelay(duration: Duration) = Channel<Unit>().receive()

        // Make send() immediate to avoid deadlock in test
        val finished = Channel<Unit>(2)
        assertThat(
            flow {
                var i = 0
                try {
                    while (true) emit(i++)
                } finally {
                    finished.send(Unit)
                }
            }.takeUntilTimeout(Duration.ofSeconds(1), ::infiniteDelay)
                .take(2)
                .toList()
        ).containsExactly(0, 1)

        finished.receive()
    }

    @Test
    @Timeout(value = testTimeoutSeconds, unit = TimeUnit.SECONDS)
    @Suppress("UNUSED_PARAMETER")
    fun `takeUntilTimeout should cancel if downstream fails`() = runBlocking<Unit> {
        suspend fun infiniteDelay(duration: Duration) = Channel<Unit>().receive()

        // Make send() immediate to avoid deadlock in test
        val finished = Channel<Unit>(2)
        assertThrows<MyException> {
            flow {
                var i = 0
                try {
                    while (true) emit(i++)
                } finally {
                    finished.send(Unit)
                }
            }.takeUntilTimeout(Duration.ofSeconds(1), ::infiniteDelay)
                .collect {
                    if (it == 2) throw MyException()
                }
        }

        finished.receive()
    }

    @Test
    fun `join on scoped coroutine in supervisor scope does not throw if the coroutine fails`() = runBlocking<Unit> {
        val scope = CoroutineScope(SupervisorJob())
        assertDoesNotThrow {
            flowOf(1).map {
                scope.launch {
                    throw Exception("boom")
                }.join()
            }.launchIn(scope)
        }
        // Uncomment to see the exception printed
//        delay(1000)
    }
}

data class ExecutionTime(
    val start: Instant,
    val end: Instant,
    val n: Int
) {
    fun contains(t: Instant) = t in start..end && t != end
}
