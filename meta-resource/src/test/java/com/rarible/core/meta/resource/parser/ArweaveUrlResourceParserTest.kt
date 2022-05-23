package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.ArweaveUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArweaveUrlResourceParserTest {
    private val arweaveUrlResourceParser = ArweaveUrlResourceParser()

    @Test
    fun `ar prefix`() {
        assertThat(arweaveUrlResourceParser.parse("ar://lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg")).isEqualTo(
            ArweaveUrl(
                original = "ar://lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg",
                originalGateway = null,
                path = "/lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg"
            )
        )
    }

    @Test
    fun `arweave net domain`() {
        assertThat(arweaveUrlResourceParser.parse("https://arweave.net/lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg")).isEqualTo(
            ArweaveUrl(
                original = "https://arweave.net/lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg",
                originalGateway = "https://arweave.net",
                path = "/lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg"
            )
        )
    }

    @Test
    fun `not ar`() {
        assertThat(arweaveUrlResourceParser.parse("https://ipfs.io/lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg")).isNull()
    }
}
