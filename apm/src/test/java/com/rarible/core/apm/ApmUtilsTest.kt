package com.rarible.core.apm

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.coroutines.coroutineContext

@ExperimentalCoroutinesApi
class ApmUtilsTest {
    @Test
    fun `withTransaction saves context`() = runBlocking<Unit> {
        val ok = withTransaction("test") {
            getApmContext() != null
        }
        assertThat(ok).isTrue()
    }

    @Test
    fun `withSpan saves new span`() = runBlocking<Unit> {
        withTransaction("initial") {
            val tx = getApmContext()?.span
            withSpan(SpanInfo("test", "bla")) {
                val otherSpan = getApmContext()?.span

                assertThat(otherSpan).isNotSameAs(tx)
                assertThat(otherSpan).isNotNull
                assertThat(tx).isNotNull
            }
        }
    }

    @Test
    fun `parent reactor context is preserved`() = runBlocking<Unit> {
        val m = mono {
            withTransaction("simple") {
                getReactorContextValue()
            }
        }
            .subscriberContext { it.put("key", 10) }
            .awaitFirst()
        assertThat(m).isEqualTo(10)
    }

    private suspend fun getReactorContextValue(): Int? {
        val ctx = coroutineContext[ReactorContext.Key]
        return ctx?.context?.get("key")
    }
}