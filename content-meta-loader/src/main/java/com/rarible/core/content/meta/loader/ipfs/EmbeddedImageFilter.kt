package com.rarible.core.content.meta.loader.ipfs

interface EmbeddedImageFilter {
    fun trigger(url: String): Boolean
}

open class EmbeddedSvgFilter : EmbeddedImageFilter {

    override fun trigger(url: String): Boolean {
        // Embedded image, return 'as is'
        return url.startsWith(SVG_START)
    }

    companion object {
        private const val SVG_START = "<svg"
    }
}


