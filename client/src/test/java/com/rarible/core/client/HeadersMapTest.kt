package com.rarible.core.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HeadersMapTest {
    @Test
    fun checkToHeadersMap() {
        val initial = mapOf(
            "testName" to "value1",
            "otherTest" to "value2",
            "traceId" to "34t3t3gg"
        )
        val test = mapOf(
            "x-log-test-name" to "value1",
            "x-log-other-test" to "value2",
            "x-log-trace-id" to "34t3t3gg"
        )
        assertThat(initial.toHeadersMap())
            .isEqualTo(test)
    }
}