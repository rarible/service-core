package com.rarible.core.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test


@ExperimentalCoroutinesApi
class CollectionsUtilsTest {

    @Test
    fun `should map over iterable concurrently`() = runBlockingTest {
        val iterable = (1..10000).toList()
        val started = this.currentTime
        val parResult = iterable.mapAsync {
            delay(1000)
            it.toString()
        }
        val ended = this.currentTime
        val simpleResult = iterable.map { it.toString() }

        Assertions.assertThat(ended - started).isEqualTo(1000)
        Assertions.assertThat(parResult).containsExactlyElementsOf(simpleResult)
    }

    @Test
    fun `should flatMap over iterable concurrently`() = runBlockingTest {
        val iterable = (1..1000).toList()
        val started = this.currentTime
        val parResult = iterable.flatMapAsync {
            delay(1000)
            (it..it+10).toList()
        }
        val ended = this.currentTime
        val simpleResult = iterable.flatMap { (it..it+10).toList() }

        Assertions.assertThat(ended - started).isEqualTo(1000)
        Assertions.assertThat(parResult).containsExactlyElementsOf(simpleResult)
    }

    @Test
    fun `should map over Map concurrently`() = runBlockingTest {
        val map = (1..10000).associateWith { "Value: $it" }
        val started = this.currentTime
        val parResult = map.mapAsync { (key, value) ->
            delay(1000)
            "$key=$value"
        }
        val ended = this.currentTime
        val simpleResult = map.map { (key, value) -> "$key=$value" }

        Assertions.assertThat(ended - started).isEqualTo(1000)
        Assertions.assertThat(parResult).containsExactlyElementsOf(simpleResult)
    }

    @Test
    fun `should flatMap over Map concurrently`() = runBlockingTest {
        val map = (1..10000).associateWith { "Value: $it" }
        val started = this.currentTime
        val parResult = map.flatMapAsync { (key, value) ->
            delay(1000)
            listOf("$key=$value")
        }
        val ended = this.currentTime
        val simpleResult = map.flatMap { (key, value) -> listOf("$key=$value") }

        Assertions.assertThat(ended - started).isEqualTo(1000)
        Assertions.assertThat(parResult).containsExactlyElementsOf(simpleResult)
    }
}