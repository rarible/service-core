package com.rarible.core.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class CollectionsUtilsTest {

    private val delay = 1000L

    @Test
    fun `should map over iterable concurrently`() = runBlocking<Unit> {
        val iterable = (1..10000).toList()
        val started = nowMillis()
        val parResult = iterable.mapAsync {
            delay(delay)
            it.toString()
        }
        val spent = nowMillis().toEpochMilli() - started.toEpochMilli()
        val simpleResult = iterable.map { it.toString() }

        assertThat(spent).isGreaterThanOrEqualTo(delay).isLessThanOrEqualTo(delay * 2)
        assertThat(parResult).containsExactlyElementsOf(simpleResult)
    }

    @Test
    fun `should flatMap over iterable concurrently`() = runBlocking<Unit> {
        val iterable = (1..1000).toList()
        val started = nowMillis()
        val parResult = iterable.flatMapAsync {
            delay(1000)
            (it..it + 10).toList()
        }
        val spent = nowMillis().toEpochMilli() - started.toEpochMilli()
        val simpleResult = iterable.flatMap { (it..it + 10).toList() }

        assertThat(spent).isGreaterThanOrEqualTo(delay).isLessThanOrEqualTo(delay * 2)
        assertThat(parResult).containsExactlyElementsOf(simpleResult)
    }

    @Test
    fun `should map over Map concurrently`() = runBlocking<Unit> {
        val map = (1..10000).associateWith { "Value: $it" }
        val started = nowMillis()
        val parResult = map.mapAsync { (key, value) ->
            delay(delay)
            "$key=$value"
        }
        val spent = nowMillis().toEpochMilli() - started.toEpochMilli()
        val simpleResult = map.map { (key, value) -> "$key=$value" }

        assertThat(spent).isGreaterThanOrEqualTo(delay).isLessThanOrEqualTo(delay * 2)
        assertThat(parResult).containsExactlyElementsOf(simpleResult)
    }

    @Test
    fun `should flatMap over Map concurrently`() = runBlocking<Unit> {
        val map = (1..10000).associateWith { "Value: $it" }
        val started = nowMillis()
        val parResult = map.flatMapAsync { (key, value) ->
            delay(delay)
            listOf("$key=$value")
        }
        val spent = nowMillis().toEpochMilli() - started.toEpochMilli()
        val simpleResult = map.flatMap { (key, value) -> listOf("$key=$value") }

        assertThat(spent).isGreaterThanOrEqualTo(delay).isLessThanOrEqualTo(delay * 2)
        assertThat(parResult).containsExactlyElementsOf(simpleResult)
    }
}