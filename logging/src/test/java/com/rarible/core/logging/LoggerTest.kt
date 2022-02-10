package com.rarible.core.logging

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class LoggerTest {

    @Test
    fun `should pick correct logger instance`() {
        Assertions.assertThat(logger.name).isEqualTo(this::class.qualifiedName)
    }

    companion object {
        val logger by Logger()
    }
}