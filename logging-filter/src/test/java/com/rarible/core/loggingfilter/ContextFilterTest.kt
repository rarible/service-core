package com.rarible.core.loggingfilter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders

class ContextFilterTest {
    @Test
    fun checkConvert() {
        val headers = HttpHeaders()
        headers["X-LOG-TEST-NAME"] = listOf("value1")
        headers["X-L-TEST-NAME"] = listOf("value1")
        headers["X-LOG-TRACE-ID"] = listOf("simple")

        val result = headers.toLoggingContext()
        val test = mapOf(
            "testName" to "value1",
            "traceId" to "simple"
        )
        assertThat(result)
            .isEqualTo(test)
    }

    @Test
    fun checkTraceIdGen() {
        val headers = HttpHeaders()
        headers["X-LOG-TEST-NAME"] = listOf("value1")
        headers["X-L-TEST-NAME"] = listOf("value1")

        val result = headers.toLoggingContext()
        assertThat(result.containsKey("traceId"))
        println(result.get("traceId"))
    }

}