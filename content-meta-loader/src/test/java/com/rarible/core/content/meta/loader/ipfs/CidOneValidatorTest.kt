package com.rarible.core.content.meta.loader.ipfs

import com.rarible.core.content.meta.loader.addressing.cid.CidOneValidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CidOneValidatorTest {

    private val resolver = CidOneValidator()

    @Test
    fun `is cid`() {
        assertThat(resolver.isCid("QmQzqPpcBFkc9AwV4B2tscLy9dBwN7o9yEHE9aRCHeN6KW")).isTrue
        assertThat(resolver.isCid("QQzqPpcBFkc9AwV4B2tscLy9dBwN7o9yEHE9aRCHeN6KW")).isFalse
        assertThat(resolver.isCid("bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi")).isTrue
        assertThat(resolver.isCid("3")).isFalse
        assertThat(resolver.isCid("f01701220c3c4733ec8affd06cf9e9ff50ffc6bcd2ec85a6170004bb709669c31de94391a")).isTrue
        // TODO Добавить кейсов
    }
}
