package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.model.SchemaUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SchemaUrlResourceParserTest {

    private val arweaveUrlResourceParser = SchemaUrlResourceParser()

    @Test
    fun `ar prefix`() {
        assertThat(arweaveUrlResourceParser.parse("ar://lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg")).isEqualTo(
            SchemaUrl(
                original = "ar://lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg",
                schema = "ar",
                path = "lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg",
                gateway = "https://arweave.net"
            )
        )
    }

    @Test
    fun `not ar`() {
        assertThat(arweaveUrlResourceParser.parse("https://ipfs.io/lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg")).isNull()
    }
}
