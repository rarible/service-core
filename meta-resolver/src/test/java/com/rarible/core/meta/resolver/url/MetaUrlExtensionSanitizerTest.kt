package com.rarible.core.meta.resolver.url

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MetaUrlExtensionSanitizerTest {

    private val entityId = "0x0"

    @Test
    fun `sanitize - ok`() {
        val sanitizer = MetaUrlExtensionSanitizer<String>(
            setOf(
                ".json",
                ".svg"
            )
        )

        assertThat(sanitizer.sanitize(entityId, "test1.json")).isEqualTo("test1")
        assertThat(sanitizer.sanitize(entityId, "test2.svg")).isEqualTo("test2")
        assertThat(sanitizer.sanitize(entityId, "test3.png")).isNull()
    }
}
