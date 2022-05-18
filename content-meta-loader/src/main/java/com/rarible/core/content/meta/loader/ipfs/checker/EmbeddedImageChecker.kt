package com.rarible.core.content.meta.loader.ipfs.checker

import com.rarible.core.content.meta.loader.ipfs.EmbeddedImageFilter
import org.springframework.stereotype.Component

@Component
class EmbeddedImageChecker(
    val embeddedImageFilters: List<EmbeddedImageFilter>
) {

    fun check(url: String): String? {
        for (filter in embeddedImageFilters) {
            if (filter.trigger(url)) return url
        }
        return null
    }
}
