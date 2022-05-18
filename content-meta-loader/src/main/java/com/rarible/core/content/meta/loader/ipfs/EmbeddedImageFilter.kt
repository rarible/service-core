package com.rarible.core.content.meta.loader.ipfs

import org.springframework.stereotype.Component

interface EmbeddedImageFilter {
    fun trigger(url: String): Boolean
}

@Component
class EmbeddedSvgFilter : EmbeddedImageFilter {

    override fun trigger(url: String): Boolean {
        // Embedded image, return 'as is'
        return url.startsWith(SVG_START)
    }

    companion object {
        private const val SVG_START = "<svg"
    }
}


