package com.rarible.core.meta.resolver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MetaMediaTypeResolverTest {
    private val resolver = MetaMediaTypeResolver()

    @Test
    fun `resolve - ipfs`() {
        val metaUrl = "ipfs://bafybeia4j7f2spcu4zf2n4ix32arhp3hyudtpzp2xpurqi77gwdqffvbny/B200.png"
        val rawMeta = RawMeta(null, ByteArray(1), null)
        val result = resolver.resolveContent(metaUrl, rawMeta)
        assertThat(result?.meta?.mimeType).isEqualTo("image/png")
    }
}
