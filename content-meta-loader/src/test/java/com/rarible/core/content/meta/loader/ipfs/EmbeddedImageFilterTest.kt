package com.rarible.core.content.meta.loader.ipfs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EmbeddedImageFilterTest {

    private val svgUriFilter = EmbeddedSvgFilter()

    @Test
    fun `filter svg prefix`() {
        assertThat(svgUriFilter.trigger("<svg lalal")).isTrue
        assertThat(svgUriFilter.trigger("http://lalal")).isFalse
    }
}
